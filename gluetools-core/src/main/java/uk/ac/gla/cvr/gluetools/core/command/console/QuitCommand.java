package uk.ac.gla.cvr.gluetools.core.command.console;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;


@CommandClass(
	commandWords={"quit"},
	docoptUsages={""},
	description="Quit GLUE"
) 
public class QuitCommand extends ConsoleCommand {

	@Override
	protected CommandResult executeOnConsole(ConsoleCommandContext cmdContext) {
		cmdContext.setFinished(true);
		return CommandResult.OK;
	}

}
