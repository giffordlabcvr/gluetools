package uk.ac.gla.cvr.gluetools.core.command.console;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;

public abstract class ConsoleCommand extends Command {

	@Override
	public final CommandResult execute(CommandContext cmdContext) {
		return executeOnConsole((ConsoleCommandContext) cmdContext);
	}

	protected abstract CommandResult executeOnConsole(ConsoleCommandContext cmdContext);
}
