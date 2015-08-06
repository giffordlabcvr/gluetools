package uk.ac.gla.cvr.gluetools.core.command.console;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;


@CommandClass(
	commandWords={"quit"},
	docoptUsages={""},
	description="Quit GLUE",
	modeWrappable = false
) 
public class QuitCommand extends ConsoleCommand<OkResult> {

	@Override
	protected OkResult executeOnConsole(ConsoleCommandContext cmdContext) {
		cmdContext.setFinished(true);
		return CommandResult.OK;
	}

}
