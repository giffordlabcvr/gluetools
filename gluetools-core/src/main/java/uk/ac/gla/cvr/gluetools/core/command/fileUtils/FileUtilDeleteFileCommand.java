package uk.ac.gla.cvr.gluetools.core.command.fileUtils;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@CommandClass(
		commandWords={"file-util", "delete-file"}, 
		description = "Delete a file", 
		docoptUsages = { "<fileName>" },
		docoptOptions = {},
		metaTags = {CmdMeta.consoleOnly}	
)
public class FileUtilDeleteFileCommand extends Command<FileUtilDeleteFileResult> {

	public static final String FILENAME = "fileName";

	private String fileName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.fileName = PluginUtils.configureStringProperty(configElem, FILENAME, true);
	}

	@Override
	public FileUtilDeleteFileResult execute(CommandContext cmdContext) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		boolean deleteResult = consoleCmdContext.delete(fileName);
		return new FileUtilDeleteFileResult(deleteResult ? 1 : 0);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("fileName", false);
		}
		
	}
	
}
