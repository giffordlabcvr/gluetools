package uk.ac.gla.cvr.gluetools.core.command;

import java.util.Arrays;

import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;

public abstract class CommandUsageGenerator {

	@SuppressWarnings("rawtypes")
	public abstract CommandUsage generateUsage(Class<? extends Command> cmdClass);

	@SuppressWarnings("rawtypes")
	public static CommandUsageGenerator commandUsageGeneratorForCmdClass(Class<? extends Command> cmdClass) {
		Class<?> commandUsageGeneratorClass = Arrays.asList(cmdClass.getClasses()).stream().
				filter(c -> c.getAnnotation(CommandUsageGeneratorClass.class) != null).findFirst().orElse(null);
		if(commandUsageGeneratorClass == null) {
			return null;
		}
		try {
			return (CommandUsageGenerator) commandUsageGeneratorClass.getConstructor().newInstance();
		} catch(Exception e) {
			GlueLogger.getGlueLogger().warning("Failed to instantiate command usage generator class "+commandUsageGeneratorClass.getCanonicalName());
			GlueLogger.getGlueLogger().warning(e.getClass().getCanonicalName()+": "+e.getMessage());
		}
		return null;
	}
}
