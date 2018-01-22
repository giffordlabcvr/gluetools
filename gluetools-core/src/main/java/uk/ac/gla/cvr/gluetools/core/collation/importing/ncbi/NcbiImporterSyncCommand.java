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
package uk.ac.gla.cvr.gluetools.core.collation.importing.ncbi;

import java.util.Optional;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ProvidedProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass( 
		commandWords={"sync"}, 
		docoptUsages={"[-d]"},
		docoptOptions={"-d, --detailed  Show detailed per-sequence status"},
		metaTags={CmdMeta.updatesDatabase},
		description="Determine present/missing/surplus, download missing, delete surplus") 
public class NcbiImporterSyncCommand extends ModulePluginCommand<CommandResult, NcbiImporter> implements ProvidedProjectModeCommand {

	public static final String DETAILED = "detailed";
	
	private boolean detailed;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.detailed = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, DETAILED, false)).orElse(false);
	}

	@Override
	protected CommandResult execute(CommandContext cmdContext, NcbiImporter importerPlugin) {
		NcbiImporterStatus ncbiImporterStatus = importerPlugin.doSync(cmdContext);
		if(detailed) {
			return new NcbiImporterDetailedResult(ncbiImporterStatus.getSequenceStatusTable());
		} else {
			return new NcbiImporterSummaryResult(ncbiImporterStatus);
		}
	}
	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		
	}

}
