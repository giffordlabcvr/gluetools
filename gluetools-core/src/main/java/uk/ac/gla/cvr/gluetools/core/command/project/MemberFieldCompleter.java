package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandCompleter;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

@SuppressWarnings("rawtypes")
public class MemberFieldCompleter extends CommandCompleter {
	@Override
	public List<String> completionSuggestions(
			ConsoleCommandContext cmdContext,
			Class<? extends Command> cmdClass, List<String> argStrings) {
		List<String> suggestions = new ArrayList<String>();
		if(argStrings.size() == 0) {
			suggestions.addAll(getMemberFieldNames(cmdContext));
		}
		return suggestions;
	}


	protected List<String> getMemberFieldNames(ConsoleCommandContext cmdContext) {
		return getProject(cmdContext).getValidMemberFields();
	}

	private Project getProject(ConsoleCommandContext cmdContext) {
		InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
		Project project = insideProjectMode.getProject();
		return project;
	}
}