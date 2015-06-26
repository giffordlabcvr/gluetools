package uk.ac.gla.cvr.gluetools.core.command;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;

public abstract class Command implements Plugin {

	public abstract CommandResult execute(CommandContext cmdContext);
	
	public static abstract class CommandCompleter {
		public abstract List<String> completionSuggestions(ConsoleCommandContext cmdContext, List<String> argStrings);
	}
	
}
