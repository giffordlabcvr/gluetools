package uk.ac.gla.cvr.gluetools.core.command;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;

public abstract class CommandCompleter {
	
	private static Logger logger = Logger.getLogger("uk.ac.gla.cvr.gluetools.core");

	public abstract List<String> completionSuggestions(ConsoleCommandContext cmdContext, 
			Class<? extends Command> cmdClass, List<String> argStrings);
	
	public static CommandCompleter commandCompleterForCmdClass(Class<? extends Command> cmdClass) {
		Class<?> completerClass = Arrays.asList(cmdClass.getClasses()).stream().
				filter(c -> c.getAnnotation(CompleterClass.class) != null).findFirst().orElse(null);
		if(completerClass == null) {
			return null;
		}
		try {
			return (CommandCompleter) completerClass.getConstructor().newInstance();
		} catch(Exception e) {
			logger.warning("Failed to instantiate command completer class "+completerClass.getCanonicalName());
			logger.warning(e.getClass().getCanonicalName()+": "+e.getMessage());
		}
		return null;
	}
}