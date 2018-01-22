/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.command;

import java.util.Arrays;

import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;

@SuppressWarnings("rawtypes")
public abstract class EnterModeCommandDescriptor {

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
				GlueLogger.getGlueLogger().warning("Failed to instantiate enter mode descriptor class "+descriptorClass.getCanonicalName());
				GlueLogger.getGlueLogger().warning(e.getClass().getCanonicalName()+": "+e.getMessage());
				return null;
			}
		} else {
			String[] docoptUsages = CommandUsage.docoptUsagesForCmdClass((Class<? extends Command>) enterModeCmdClass);
			String mainDocoptUsage = docoptUsages[0];
			// main usage contains items indicating optional elements.
			if(mainDocoptUsage.contains("[") || mainDocoptUsage.contains("]")) {
				GlueLogger.getGlueLogger().warning(enterModeCmdClass.getSimpleName()+" has optional docopt, cannot be used as a mode changing command.");
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
