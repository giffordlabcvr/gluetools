package uk.ac.gla.cvr.gluetools.core.command.console;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandMode;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;

@PluginClass(elemName="exit")
@CommandClass(description="Exit current command mode and return to parent mode", docoptUsages={""}) 
public class ExitCommand extends Command {

	@Override
	public CommandResult execute(CommandContext cmdContext) {
		if(cmdContext.peekCommandMode() != CommandMode.ROOT) {
			cmdContext.popCommandMode();
		}
		return CommandResult.OK;
	}

}
