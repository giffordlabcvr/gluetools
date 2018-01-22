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

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;

@SuppressWarnings("rawtypes")
public abstract class CommandCompleter {
	
	public List<CompletionSuggestion> completionSuggestions(ConsoleCommandContext cmdContext, 
			Class<? extends Command> cmdClass, List<String> argStrings, String prefix, boolean includeOptions) {
		List<String> completionSuggestions = completionSuggestions(cmdContext, cmdClass, argStrings, includeOptions);
		return completionSuggestions.stream().map(s -> new CompletionSuggestion(s, true)).collect(Collectors.toList());
	}
	
	
	public List<String> completionSuggestions(ConsoleCommandContext cmdContext, 
			Class<? extends Command> cmdClass, List<String> argStrings, boolean includeOptions) {
		return Collections.emptyList();
	}
	
	@SuppressWarnings("rawtypes")
	public static CommandCompleter commandCompleterForCmdClass(Class<? extends Command> cmdClass) {
		Class<?> completerClass = Arrays.asList(cmdClass.getClasses()).stream().
				filter(c -> c.getAnnotation(CompleterClass.class) != null).findFirst().orElse(null);
		if(completerClass == null) {
			return null;
		}
		try {
			Constructor<?> constructor = completerClass.getConstructor();
			return (CommandCompleter) constructor.newInstance();
		} catch(Exception e) {
			GlueLogger.getGlueLogger().warning("Failed to instantiate command completer class "+completerClass.getCanonicalName());
			GlueLogger.getGlueLogger().warning(e.getClass().getCanonicalName()+": "+e.getMessage());
		}
		return null;
	}
}