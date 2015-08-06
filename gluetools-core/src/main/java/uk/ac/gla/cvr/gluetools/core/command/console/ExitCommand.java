package uk.ac.gla.cvr.gluetools.core.command.console;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.command.root.RootCommandMode;


@CommandClass( 
	commandWords={"exit"},
	docoptUsages={""},
	description="Exit current command mode", 
	modeWrappable=false) 
public class ExitCommand extends Command<OkResult> {

	@Override
	public OkResult execute(CommandContext cmdContext) {
		if(!(cmdContext.peekCommandMode() instanceof RootCommandMode)) {
			cmdContext.popCommandMode();
		}
		return CommandResult.OK;
	}

}
