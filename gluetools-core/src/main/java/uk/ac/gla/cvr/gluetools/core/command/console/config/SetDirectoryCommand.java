package uk.ac.gla.cvr.gluetools.core.command.console.config;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.SimpleConsoleCommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
		commandWords = {"console","set","directory"},
		docoptUsages = {"<path>"}, 
		modeWrappable = false,
		description = "Set the directory path for loading and saving",
		furtherHelp = "An absolute <path> replaces the current setting. A relative <path> updates the setting relative to its current value.")
public class SetDirectoryCommand extends ConsoleCommand {

	private String path;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		path = PluginUtils.configureStringProperty(configElem, "path", true);
	}

	@Override
	protected CommandResult executeOnConsole(ConsoleCommandContext cmdContext) {
		cmdContext.updateLoadSavePath(path);
		final String path = cmdContext.getLoadSavePath().getAbsolutePath();
		return new SimpleConsoleCommandResult(path);
	}

}