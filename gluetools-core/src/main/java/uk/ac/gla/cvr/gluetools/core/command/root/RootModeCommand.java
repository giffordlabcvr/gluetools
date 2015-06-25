package uk.ac.gla.cvr.gluetools.core.command.root;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

public abstract class RootModeCommand extends Command {

	protected Project getProject(ObjectContext objContext, String projectName) {
		return GlueDataObject.lookup(objContext, Project.class, Project.pkMap(projectName));
	}
	
	protected abstract static class ProjectNameCompleter extends CommandCompleter {

		@Override
		public List<String> completionSuggestions(CommandContext cmdContext, List<String> argStrings) {
			LinkedList<String> suggestions = new LinkedList<String>();
			if(argStrings.isEmpty()) {
				suggestions.addAll(CommandUtils.runListCommand(cmdContext, Project.class, new SelectQuery(Project.class)).
						getResults().stream().map(Project::getName).collect(Collectors.toList()));
			}
			return suggestions;
		}
		
	}

	
}
