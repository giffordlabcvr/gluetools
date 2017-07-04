package uk.ac.gla.cvr.gluetools.core.command.console;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.DateUtils;


@CommandClass(
	commandWords={"run", "script"},
	docoptUsages={"<filePath>"},
	docoptOptions={},
	description="Run Nashorn JS script from a file",
	metaTags = { CmdMeta.consoleOnly, CmdMeta.updatesDatabase }
) 
public class RunScriptCommand extends Command<OkResult> {

	
	private String filePath;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.filePath = PluginUtils.configureStringProperty(configElem, "filePath", true);
	}



	@Override
	public OkResult execute(CommandContext cmdContext) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		long startTime = System.currentTimeMillis();
		GlueLogger.getGlueLogger().finest("Started running script "+filePath);
		String scriptContent = new String(consoleCmdContext.loadBytes(filePath));
		consoleCmdContext.runScript(filePath, scriptContent);
		long milliseconds = System.currentTimeMillis()-startTime;
		GlueLogger.getGlueLogger().finest("Completed script "+filePath+", time taken: "+DateUtils.formatDuration(milliseconds));
		return CommandResult.OK;
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("filePath", false);
		}
	}
}
