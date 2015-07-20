package uk.ac.gla.cvr.gluetools.core.command.console;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;


@CommandClass(
	commandWords={"quit"},
	docoptUsages={""},
	description="Quit GLUE",
	modeWrappable = false
) 
public class QuitCommand extends ConsoleCommand {

	@Override
	protected CommandResult executeOnConsole(ConsoleCommandContext cmdContext) {
		cmdContext.setFinished(true);
		return CommandResult.OK;
	}

}
