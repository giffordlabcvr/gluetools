package uk.ac.gla.cvr.gluetools.core.command.console;

import uk.ac.gla.cvr.gluetools.core.command.CommandResult;

public abstract class ConsoleCommandResult extends CommandResult {

	public abstract String getResultAsConsoleText();
}
