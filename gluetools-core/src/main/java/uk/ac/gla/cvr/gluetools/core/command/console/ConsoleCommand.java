package uk.ac.gla.cvr.gluetools.core.command.console;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;

public abstract class ConsoleCommand<R extends CommandResult> extends Command<R> {

	@Override
	public final R execute(CommandContext cmdContext) {
		return executeOnConsole((ConsoleCommandContext) cmdContext);
	}

	protected abstract R executeOnConsole(ConsoleCommandContext cmdContext);
}
