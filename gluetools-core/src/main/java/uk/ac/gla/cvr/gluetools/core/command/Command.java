package uk.ac.gla.cvr.gluetools.core.command;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;

public abstract class Command implements Plugin {

	public abstract CommandResult execute(CommandContext cmdContext);
	
}
