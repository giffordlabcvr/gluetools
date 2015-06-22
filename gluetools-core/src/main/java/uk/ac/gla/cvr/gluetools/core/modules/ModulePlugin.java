package uk.ac.gla.cvr.gluetools.core.modules;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;

public interface ModulePlugin extends Plugin {
	
	
	public CommandResult runModule(CommandContext cmdContext);
	
	
}
