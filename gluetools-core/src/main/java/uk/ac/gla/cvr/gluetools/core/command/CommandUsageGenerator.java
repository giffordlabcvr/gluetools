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
