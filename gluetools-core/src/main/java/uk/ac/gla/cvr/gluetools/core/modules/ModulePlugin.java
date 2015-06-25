package uk.ac.gla.cvr.gluetools.core.modules;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectMode;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;

public abstract class ModulePlugin implements Plugin {
	
	
	public abstract CommandResult runModule(CommandContext cmdContext);
	
	public ProjectMode getProjectMode(CommandContext cmdContext) {
		return (ProjectMode) cmdContext.peekCommandMode();
	}
}
