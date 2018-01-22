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
package uk.ac.gla.cvr.gluetools.core.digs.importer;

import java.util.Optional;

import org.apache.cayenne.exp.Expression;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.source.Source;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"import", "extracted"}, 
		description = "Import rows from a DIGS \"Extracted\" table as GLUE sequences", 
		docoptUsages = { "<digsDbName> [-s <sourceName>] [-w <whereClause>]" },
		docoptOptions = {
				"-s <sourceName>, --sourceName <sourceName>     Source to contain sequences",
				"-w <whereClause>, --whereClause <whereClause>  Qualify imported rows"
		},
		metaTags = {}, 
		furtherHelp = 
		"If <sourceName> is supplied, a source with this name will be created if necessary, "+
		"and the new sequences will be added to this source. If <sourceName> is omitted, the "+
		"default source name is the DIGS DB name.\n"+
		"Example:\n"+
		"  import extracted TD_Heterocephalus_RT -w \"scaffold = 'JH602050' and sequenceLength > 250\""
)
public class ImportExtractedCommand extends DigsImporterCommand<CreateResult> implements ProvidedProjectModeCommand {

	public static final String DIGS_DB_NAME = "digsDbName";
	public static final String SOURCE_NAME = "sourceName";
	public static final String WHERE_CLAUSE = "whereClause";

	
	private String digsDbName;
	private String sourceName;
	private Optional<Expression> whereClause;
	

	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.digsDbName = PluginUtils.configureStringProperty(configElem, DIGS_DB_NAME, true);
		this.sourceName = PluginUtils.configureStringProperty(configElem, SOURCE_NAME, false);
		if(this.sourceName == null) {
			this.sourceName = digsDbName;
		}
		this.whereClause = Optional.ofNullable(PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false));
	}

	@Override
	protected CreateResult execute(CommandContext cmdContext, DigsImporter digsImporter) {
		return digsImporter.importHits(cmdContext, digsDbName, sourceName, whereClause);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerVariableInstantiator("digsDbName", new DigsDbNameInstantiator());
			registerDataObjectNameLookup("sourceName", Source.class, Source.NAME_PROPERTY);
		}
		
	}
	
	
	
	
}
