package uk.ac.gla.cvr.gluetools.core.command.console.config;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.SimpleConsoleCommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
		commandWords = {"console","change","load-save-path"},
		docoptUsages = {"<path>"}, 
		modeWrappable = false,
		description = "Change the path for loading and saving file",
		furtherHelp = "An absolute <path> replaces the load-save-path option value. A relative <path> updates the path relative to its current value")
public class ConsoleChangeDirectoryCommand extends ConsoleCommand<SimpleConsoleCommandResult> {

	private String path;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		path = PluginUtils.configureStringProperty(configElem, "path", true);
	}

	@Override
	protected SimpleConsoleCommandResult executeOnConsole(ConsoleCommandContext cmdContext) {
		cmdContext.updateLoadSavePath(path);
		final String path = cmdContext.getLoadSavePath().getAbsolutePath();
		return new SimpleConsoleCommandResult(path);
	}

}