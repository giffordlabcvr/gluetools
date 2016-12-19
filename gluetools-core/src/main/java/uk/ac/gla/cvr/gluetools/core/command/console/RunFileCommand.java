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
	commandWords={"run", "file"},
	docoptUsages={"[-E] [-C] [-O] <filePath>"},
	docoptOptions={
			"-E, --no-cmd-echo      Suppress batch command echo",
			"-C, --no-comment-echo  Suppress batch comment echo",
	   		"-O, --no-output        Suppress batch result output"},
	description="Run batch commands from a file",
	metaTags = { CmdMeta.consoleOnly, CmdMeta.updatesDatabase }
) 
public class RunFileCommand extends Command<OkResult> {

	
	private String filePath;
	private boolean noCmdEcho;
	private boolean noCommentEcho;
	private boolean noOutput;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.filePath = PluginUtils.configureStringProperty(configElem, "filePath", true);
		this.noCmdEcho = PluginUtils.configureBooleanProperty(configElem, "no-cmd-echo", true);
		this.noCommentEcho = PluginUtils.configureBooleanProperty(configElem, "no-comment-echo", true);
		this.noOutput = PluginUtils.configureBooleanProperty(configElem, "no-output", true);
	}



	@Override
	public OkResult execute(CommandContext cmdContext) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		long startTime = System.currentTimeMillis();
		GlueLogger.getGlueLogger().finest("Started running GLUE batch "+filePath);
		String batchContent = new String(consoleCmdContext.loadBytes(filePath));
		consoleCmdContext.runBatchCommands(filePath, batchContent, noCmdEcho, noCommentEcho, noOutput);
		long milliseconds = System.currentTimeMillis()-startTime;
		GlueLogger.getGlueLogger().finest("Completed GLUE batch "+filePath+", time taken: "+DateUtils.formatDuration(milliseconds));
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
