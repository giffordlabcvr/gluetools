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