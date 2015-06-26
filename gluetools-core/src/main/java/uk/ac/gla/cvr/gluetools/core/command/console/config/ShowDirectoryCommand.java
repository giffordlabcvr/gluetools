package uk.ac.gla.cvr.gluetools.core.command.console.config;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandResult;


@CommandClass( 
		commandWords = {"console","show","directory"},
		docoptUsages = {""}, 
		modeWrappable = false,
		description = "Show the current directory for loading and saving")
public class ShowDirectoryCommand extends ConsoleCommand {

	@Override
	protected CommandResult executeOnConsole(ConsoleCommandContext cmdContext) {
		final String path = cmdContext.getLoadSavePath().getAbsolutePath();
		return new ConsoleCommandResult() {
			@Override
			public String getResultAsConsoleText() {
				return path;
			}
		};
	}

}