package uk.ac.gla.cvr.gluetools.core.command.console;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;


@CommandClass(
	commandWords={"quit"},
	docoptUsages={""},
	description="Quit GLUE",
	metaTags = { CmdMeta.consoleOnly, CmdMeta.nonModeWrappable }
) 
public class QuitCommand extends Command<OkResult> {

	@Override
	public OkResult execute(CommandContext cmdContext) {
		ConsoleCommandContext consoleCommandContext = (ConsoleCommandContext) cmdContext;
		consoleCommandContext.setFinished(true);
		return CommandResult.OK;
	}

}
