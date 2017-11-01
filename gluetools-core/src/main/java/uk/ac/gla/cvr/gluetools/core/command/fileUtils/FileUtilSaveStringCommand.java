package uk.ac.gla.cvr.gluetools.core.command.fileUtils;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"file-util", "save-string"}, 
		description = "Save a string to a file", 
		docoptUsages = { "<string> <fileName>" },
		docoptOptions = {},
		metaTags = {CmdMeta.consoleOnly}	
)
public class FileUtilSaveStringCommand extends Command<OkResult> {

	public static final String STRING = "string";
	public static final String FILENAME = "fileName";

	private String string;
	private String fileName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.string = PluginUtils.configureStringProperty(configElem, STRING, true);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILENAME, true);
	}

	@Override
	public OkResult execute(CommandContext cmdContext) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		consoleCmdContext.saveBytes(fileName, string.getBytes());
		return new OkResult();
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("fileName", false);
		}
		
	}
	
} 