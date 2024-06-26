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
package uk.ac.gla.cvr.gluetools.core.collation.populating.textfile;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"populate"}, 
		docoptUsages={"[-b <batchSize>] [-w <whereClause>] [-p] -f <fileName> [<property> ...]"},
		docoptOptions={
				"-b <batchSize>, --batchSize <batchSize>        Commit batch size [default: 250]",
				"-p, --preview                                  Preview only, no DB updates",
				"-w <whereClause>, --whereClause <whereClause>  Qualify updated sequences",
				"-f <fileName>, --fileName <fileName>           Text file with field values"
		},
		description="Populate sequence field values based on a text file", 
		metaTags = { CmdMeta.consoleOnly},
		furtherHelp="The file is loaded from a location relative to the current load/save directory."+
		"The <batchSize> argument allows you to control how often updates are committed to the database "+
				"during the import. The default is every 250 text file lines. A larger <batchSize> means fewer database "+
				"accesses, but requires more Java heap memory."+
				"If <property> arguments are supplied, the populator will not update any property unless it appears in the <property> list. "+
				"If no <property> arguments are supplied, the populator may update any field.") 
public class TextFilePopulatorPopulateCommand extends ModulePluginCommand<TextFilePopulatorResult, TextFilePopulator> implements ProvidedProjectModeCommand {

	public static final String WHERE_CLAUSE = "whereClause";
	public static final String PROPERTY = "property";


	private Integer batchSize;
	private Boolean preview;
	private String fileName;
	private List<String> properties;
	private Optional<Expression> whereClause;
	
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		batchSize = Optional.ofNullable(PluginUtils.configureIntProperty(configElem, "batchSize", false)).orElse(250);
		whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
		fileName = PluginUtils.configureStringProperty(configElem, "fileName", true);
		preview = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, "preview", false)).orElse(false);
		properties = PluginUtils.configureStringsProperty(configElem, PROPERTY);
		if(properties.isEmpty()) {
			properties = null; // default fields
		}
	}
	
	@Override
	protected TextFilePopulatorResult execute(CommandContext cmdContext, TextFilePopulator populatorPlugin) {
		return new TextFilePopulatorResult(populatorPlugin.populate((ConsoleCommandContext) cmdContext, fileName, batchSize, whereClause, preview, properties));
	}
	
	@CompleterClass
	public static class Completer extends ModuleCmdCompleter<TextFilePopulator> {
		public Completer() {
			super();
			registerPathLookup("fileName", false);
			registerVariableInstantiator("property", new ModuleVariableInstantiator() {
				@SuppressWarnings("rawtypes")
				@Override
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext, TextFilePopulator textFilePopulator,
						Class<? extends Command> cmdClass, Map<String, Object> bindings,
						String prefix) {
					return textFilePopulator.allUpdatablePropertyPaths()
							.stream()
							.map(str -> new CompletionSuggestion(str, true))
							.collect(Collectors.toList());
				}
			});
		}
	}

	
}
