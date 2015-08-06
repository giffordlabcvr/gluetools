package uk.ac.gla.cvr.gluetools.core.command.root;

import java.util.LinkedList;
import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

public abstract class RootModeCommand<R extends CommandResult> extends Command<R> {

	protected Project getProject(ObjectContext objContext, String projectName) {
		return GlueDataObject.lookup(objContext, Project.class, Project.pkMap(projectName));
	}
	
	@SuppressWarnings("rawtypes")
	protected abstract static class ProjectNameCompleter extends CommandCompleter {
		@Override
		public List<String> completionSuggestions(ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass, List<String> argStrings) {
			LinkedList<String> suggestions = new LinkedList<String>();
			if(argStrings.isEmpty()) {
				ListResult listCmdResult = CommandUtils.runListCommand(cmdContext, Project.class, new SelectQuery(Project.class));
				suggestions.addAll(listCmdResult.getColumnValues(Project.NAME_PROPERTY));
			}
			return suggestions;
		}
		
	}

	
}
