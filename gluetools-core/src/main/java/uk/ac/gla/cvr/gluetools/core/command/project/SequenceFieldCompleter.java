package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandCompleter;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;

@SuppressWarnings("rawtypes")
public class SequenceFieldCompleter extends CommandCompleter {
	@Override
	public List<String> completionSuggestions(
			ConsoleCommandContext cmdContext,
			Class<? extends Command> cmdClass, List<String> argStrings) {
		if(argStrings.size() == 0) {
			return getCustomFieldNames(cmdContext);
		}
		return super.completionSuggestions(cmdContext, cmdClass, argStrings);
	}

	protected List<String> getCustomFieldNames(ConsoleCommandContext cmdContext) {
		return getProject(cmdContext).getCustomSequenceFieldNames();
	}

	protected List<String> getAllFieldNames(ConsoleCommandContext cmdContext) {
		return getProject(cmdContext).getAllSequenceFieldNames();
	}

	private Project getProject(ConsoleCommandContext cmdContext) {
		InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
		Project project = insideProjectMode.getProject();
		return project;
	}
}