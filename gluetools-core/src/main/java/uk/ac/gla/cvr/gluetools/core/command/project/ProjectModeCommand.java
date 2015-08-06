package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.LinkedList;
import java.util.List;

import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandUtils;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;

public abstract class ProjectModeCommand<R extends CommandResult> extends Command<R> {

	
	protected ProjectMode getProjectMode(CommandContext cmdContext) {
		ProjectMode projectMode = (ProjectMode) cmdContext.peekCommandMode();
		return projectMode;
	}

	
	@SuppressWarnings("rawtypes")
	public abstract static class ModuleNameCompleter extends CommandCompleter {
		@Override
		public List<String> completionSuggestions(ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass, List<String> argStrings) {
			LinkedList<String> suggestions = new LinkedList<String>();
			if(argStrings.isEmpty()) {
				suggestions.addAll(CommandUtils.runListCommand(cmdContext, Module.class, new SelectQuery(Module.class)).
						getColumnValues(Module.NAME_PROPERTY));
			}
			return suggestions;
		}
	}

	@SuppressWarnings("rawtypes")
	public abstract static class AlignmentNameCompleter extends CommandCompleter {
		@Override
		public List<String> completionSuggestions(ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass, List<String> argStrings) {
			LinkedList<String> suggestions = new LinkedList<String>();
			if(argStrings.isEmpty()) {
				suggestions.addAll(CommandUtils.runListCommand(cmdContext, Alignment.class, new SelectQuery(Alignment.class)).
						getColumnValues(Alignment.NAME_PROPERTY));
			}
			return suggestions;
		}
	}

	@SuppressWarnings("rawtypes")
	public abstract static class RefSeqNameCompleter extends CommandCompleter {
		@Override
		public List<String> completionSuggestions(ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass, List<String> argStrings) {
			LinkedList<String> suggestions = new LinkedList<String>();
			if(argStrings.isEmpty()) {
				suggestions.addAll(CommandUtils.runListCommand(cmdContext, ReferenceSequence.class, new SelectQuery(ReferenceSequence.class)).
						getColumnValues(ReferenceSequence.NAME_PROPERTY));
			}
			return suggestions;
		}
	}

}
