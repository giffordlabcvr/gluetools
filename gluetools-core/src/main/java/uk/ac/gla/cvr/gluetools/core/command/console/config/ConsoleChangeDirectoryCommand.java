package uk.ac.gla.cvr.gluetools.core.command.console.config;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.SimpleConsoleCommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
		commandWords = {"console","change","load-save-path"},
		docoptUsages = {"<path>"}, 
		metaTags = { CmdMeta.consoleOnly, CmdMeta.nonModeWrappable },
		description = "Change the path for loading and saving file",
		furtherHelp = "An absolute <path> replaces the load-save-path option value. A relative <path> updates the path relative to its current value")
public class ConsoleChangeDirectoryCommand extends Command<SimpleConsoleCommandResult> {

	private String path;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		path = PluginUtils.configureStringProperty(configElem, "path", true);
	}

	@Override
	public SimpleConsoleCommandResult execute(CommandContext cmdContext) {
		ConsoleCommandContext consoleCommandContext = (ConsoleCommandContext) cmdContext;
		consoleCommandContext.updateLoadSavePath(path);
		final String path = consoleCommandContext.getLoadSavePath().getAbsolutePath();
		return new SimpleConsoleCommandResult(path);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerPathLookup("path", true);
		}
	}
}