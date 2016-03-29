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
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass(
	commandWords={"run", "file"},
	docoptUsages={"[-E] [-O] <filePath>"},
	docoptOptions={"-E, --no-echo    Suppress batch command echo",
	   "-O, --no-output  Suppress batch command output"},
	description="Run batch commands from a file",
	metaTags = { CmdMeta.consoleOnly, CmdMeta.updatesDatabase }
) 
public class RunFileCommand extends Command<OkResult> {

	
	private String filePath;
	private boolean noEcho;
	private boolean noOutput;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.filePath = PluginUtils.configureStringProperty(configElem, "filePath", true);
		this.noEcho = PluginUtils.configureBooleanProperty(configElem, "no-echo", true);
		this.noOutput = PluginUtils.configureBooleanProperty(configElem, "no-output", true);
	}



	@Override
	public OkResult execute(CommandContext cmdContext) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		String batchContent = new String(consoleCmdContext.loadBytes(filePath));
		consoleCmdContext.runBatchCommands(filePath, batchContent, noEcho, noOutput);
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
