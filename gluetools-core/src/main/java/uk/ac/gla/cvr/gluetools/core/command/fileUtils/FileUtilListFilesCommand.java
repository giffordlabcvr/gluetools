package uk.ac.gla.cvr.gluetools.core.command.fileUtils;

import java.util.List;

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
		commandWords={"file-util", "list-files"}, 
		description = "List files in a directory", 
		docoptUsages = { "[-d <directory>]" },
		docoptOptions = { 
				"-d <directory>, --directory <directory>  Directory name",
		},
		furtherHelp = "If <directory> is omitted, the console load-save path is used. "+
				"If <directory> is relative, it is relative to the console load-save path.",
		metaTags = {CmdMeta.consoleOnly}	
)
public class FileUtilListFilesCommand extends Command<FileUtilListFilesResult> {

	public static final String DIRECTORY = "directory";

	private String directory;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.directory = PluginUtils.configureStringProperty(configElem, DIRECTORY, false);
	}

	@Override
	public FileUtilListFilesResult execute(CommandContext cmdContext) {
		ConsoleCommandContext consoleCmdContext = (ConsoleCommandContext) cmdContext;
		List<String> fileMembers;
		if(this.directory == null) {
			fileMembers = consoleCmdContext.listMembers(true, false, null);
		} else {
			fileMembers = consoleCmdContext.listMembers(directory, true, false, null);
		}
		return new FileUtilListFilesResult(fileMembers);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("directory", true);
		}
		
	}
	
}
