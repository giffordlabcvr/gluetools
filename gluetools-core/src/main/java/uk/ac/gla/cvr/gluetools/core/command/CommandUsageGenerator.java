package uk.ac.gla.cvr.gluetools.core.command;

import java.util.Arrays;
import java.util.logging.Logger;

public abstract class CommandUsageGenerator {

	private static Logger logger = Logger.getLogger("uk.ac.gla.cvr.gluetools.core");

	public abstract CommandUsage generateUsage(Class<? extends Command> cmdClass);

	public static CommandUsageGenerator commandUsageGeneratorForCmdClass(Class<? extends Command> cmdClass) {
		Class<?> commandUsageGeneratorClass = Arrays.asList(cmdClass.getClasses()).stream().
				filter(c -> c.getAnnotation(CommandUsageGeneratorClass.class) != null).findFirst().orElse(null);
		if(commandUsageGeneratorClass == null) {
			return null;
		}
		try {
			return (CommandUsageGenerator) commandUsageGeneratorClass.getConstructor().newInstance();
		} catch(Exception e) {
			logger.warning("Failed to instantiate command usage generator class "+commandUsageGeneratorClass.getCanonicalName());
			logger.warning(e.getClass().getCanonicalName()+": "+e.getMessage());
		}
		return null;
	}
}
