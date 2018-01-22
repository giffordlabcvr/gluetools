/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.collation.populating.genbank;

import java.util.List;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"populate"}, 
		docoptUsages={"[-b <batchSize>] [(-p | -s)] [-w <whereClause>] [<property> ...]"},
		docoptOptions={
				"-w <whereClause>, --whereClause <whereClause>  Qualify updated sequences",
				"-b <batchSize>, --batchSize <batchSize>        Commit batch size [default: 250]",
				"-p, --preview                                  Database will not be updated",
				"-s, --silent                                   No result table"
		},
		metaTags={CmdMeta.updatesDatabase},
		description="Populate sequence field values based on Genbank XML",
		furtherHelp=
		"The <batchSize> argument allows you to control how often updates are committed to the database "+
				"during the import. The default is every 250 sequences. A larger <batchSize> means fewer database "+
				"accesses, but requires more Java heap memory. "+
				"If <property> arguments are supplied, the populator will not update any property unless it appears in the <property> list. "+
				"If no <property> arguments are supplied, the populator may update any property.") 
public class GenbankXmlPopulatorPopulateCommand extends ModulePluginCommand<CommandResult, GenbankXmlPopulator> implements ProvidedProjectModeCommand {

	public static final String BATCH_SIZE = "batchSize";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String PROPERTY = "property";
	public static final String PREVIEW = "preview";
	public static final String SILENT = "silent";

	private Integer batchSize;
	private Optional<Expression> whereClause;
	private List<String> updatableProperties;
	private Boolean preview;
	private Boolean silent;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		batchSize = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, BATCH_SIZE, false)).orElse(250);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		preview = PluginUtils.configureBooleanProperty(configElem, PREVIEW, true);
		silent = PluginUtils.configureBooleanProperty(configElem, SILENT, true);
		updatableProperties = PluginUtils.configureStringsProperty(configElem, PROPERTY);
		if(updatableProperties.isEmpty()) {
			updatableProperties = null; // default properties
		}
		if(preview && silent) {
			throw new CommandException(Code.COMMAND_USAGE_ERROR, "At most one of --preview and --silent may be used");
		}
	}

	@Override
	protected CommandResult execute(CommandContext cmdContext, GenbankXmlPopulator populatorPlugin) {
		return populatorPlugin.populate(cmdContext, batchSize, whereClause, !preview, silent, updatableProperties);
	}
	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("property", new ModifiablePropertyInstantiator(ConfigurableTable.sequence.name()));
		}
	}
	
}
