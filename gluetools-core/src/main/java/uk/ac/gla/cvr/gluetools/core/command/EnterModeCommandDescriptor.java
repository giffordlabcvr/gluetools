package uk.ac.gla.cvr.gluetools.core.command;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public abstract class EnterModeCommandDescriptor {

	private static Logger logger = Logger.getLogger("uk.ac.gla.cvr.gluetools.core");

	/**
	 * Used to distinguish an EnterModeCommand's arguments from the words
	 * of a command which may be executed within that mode, on the same command line.
	 * Given all the arguments following the enter mode command's command words on the 
	 * current command line, return the number of arguments at the beginning which 
	 * belong to the EnterModeCommand.
	 * @param commandWords
	 */
	public abstract int numEnterModeArgs(List<String> argStrings);

	public static EnterModeCommandDescriptor getDescriptorForClass(Class<? extends EnterModeCommand> enterModeCmdClass) {
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
			@SuppressWarnings("unchecked")
			Class<? extends Command> cmdClass = (Class<? extends Command>) enterModeCmdClass;
			String[] docoptUsages = CommandUsage.docoptUsagesForCmdClass(cmdClass);
			String mainDocoptUsage = docoptUsages[0];
			// main usage contains items indicating optional elements.
			if(mainDocoptUsage.contains("[") || mainDocoptUsage.contains("]")) {
				return null;
			}
			// assumes space always separates arguments in main usage -- could relax this assumption if necessary.
			int numArgs;
			if(mainDocoptUsage.trim().length() == 0) {
				numArgs = 0;
			} else {
				numArgs = mainDocoptUsage.split(" ").length;
			}
			return new FixedUsageEnterModeCommandDescriptor(numArgs);
		}
	}
	
	/**
	 * Implementation used for mode commands with a fixed argument usage.
	 */
	public static class FixedUsageEnterModeCommandDescriptor extends EnterModeCommandDescriptor {
		private int numWords;

		public FixedUsageEnterModeCommandDescriptor(int numWords) {
			super();
			this.numWords = numWords;
		}

		@Override
		public int numEnterModeArgs(List<String> commandWords) { return numWords; }
	}
}
