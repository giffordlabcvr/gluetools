package uk.ac.gla.cvr.gluetools.core.command;

import java.util.Arrays;
import java.util.logging.Logger;

public abstract class EnterModeCommandDescriptor {

	private static Logger logger = Logger.getLogger("uk.ac.gla.cvr.gluetools.core");

	/**
	 * Used to distinguish an EnterModeCommand's arguments from the words
	 * of a command which may be executed within that mode, on the same command line.
	 * Returns the number of arguments at the beginning which belong to the EnterModeCommand.
	 * This must always be fixed for EnterMode commands.
	 * @param commandWords
	 */
	public abstract String[] enterModeArgNames();

	public static EnterModeCommandDescriptor getDescriptorForClass(Class<? extends Command> enterModeCmdClass) {
		Class<?> descriptorClass = Arrays.asList(enterModeCmdClass.getClasses()).stream().
				filter(c -> c.getAnnotation(EnterModeCommandDescriptorClass.class) != null).findFirst().orElse(null);
		if(descriptorClass != null) {
			try {
				return (EnterModeCommandDescriptor) descriptorClass.getConstructor().newInstance();
			} catch(Exception e) {
				logger.warning("Failed to instantiate enter mode descriptor class "+descriptorClass.getCanonicalName());
				logger.warning(e.getClass().getCanonicalName()+": "+e.getMessage());
				return null;
			}
		} else {
			String[] docoptUsages = CommandUsage.docoptUsagesForCmdClass((Class<? extends Command>) enterModeCmdClass);
			String mainDocoptUsage = docoptUsages[0];
			// main usage contains items indicating optional elements.
			if(mainDocoptUsage.contains("[") || mainDocoptUsage.contains("]")) {
				logger.warning(enterModeCmdClass.getSimpleName()+" has optional docopt, cannot be used as a mode changing command.");
				return null;
			}
			// assumes space always separates arguments in main usage -- could relax this assumption if necessary.
			String[] argNames;
			if(mainDocoptUsage.trim().length() == 0) {
				argNames = new String[]{};
			} else {
				argNames = mainDocoptUsage.replaceAll("[<>]", "").split(" ");
			}
			return new FixedUsageEnterModeCommandDescriptor(argNames);
		}
	}
	
	/**
	 * Implementation used for mode commands with a fixed argument usage.
	 */
	public static class FixedUsageEnterModeCommandDescriptor extends EnterModeCommandDescriptor {
		private String[] argNames;

		public FixedUsageEnterModeCommandDescriptor(String[] argNames) {
			super();
			this.argNames = argNames;
		}

		@Override
		public String[] enterModeArgNames() { return argNames; }
	}
}
