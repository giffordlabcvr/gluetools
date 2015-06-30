package uk.ac.gla.cvr.gluetools.core.command.console;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.root.RootCommandMode;


@CommandClass( 
	commandWords={"exit"},
	docoptUsages={""},
	description="Exit current command mode", 
	modeWrappable=false) 
public class ExitCommand extends Command {

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		if(!(cmdContext.peekCommandMode() instanceof RootCommandMode)) {
			cmdContext.popCommandMode();
		}
		return CommandResult.OK;
	}

}
