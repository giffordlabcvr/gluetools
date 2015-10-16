package uk.ac.gla.cvr.gluetools.core.command.root;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

public abstract class RootModeCommand<R extends CommandResult> extends Command<R> {
	
	@SuppressWarnings("rawtypes")
	protected abstract static class ProjectNameCompleter extends AdvancedCmdCompleter {
		
		public ProjectNameCompleter() {
			super();
			registerDataObjectNameLookup("projectName", Project.class, Project.NAME_PROPERTY);
		}
		
	}
	
}
