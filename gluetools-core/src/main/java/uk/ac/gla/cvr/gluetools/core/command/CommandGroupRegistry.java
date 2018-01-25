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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

public class CommandGroupRegistry {

	private Map<CommandGroup, TreeSet<Class<?>>> cmdGroupToCmdClasses = new LinkedHashMap<CommandGroup, TreeSet<Class<?>>>();

	private Map<Class<?>, CommandGroup> cmdClassToCmdGroup = new LinkedHashMap<Class<?>, CommandGroup>();

	// set this before registering a set of commands.
	// these commands will be added to the relevant group
	// for documentation purposes.
	private CommandGroup cmdGroup = null;

	@SuppressWarnings("rawtypes")
	public void registerCommandClass(Class<? extends Command> cmdClass) {
		CommandGroup cmdGroupToUse = this.cmdGroup;
		if(cmdGroupToUse == null) {
			cmdGroupToUse = CommandGroup.OTHER;
		}
		
		for(CommandGroup cmdGroup: cmdGroupToCmdClasses.keySet()) {
			if(cmdGroup.getId().equals(cmdGroupToUse.getId())) {
				cmdGroupToUse = cmdGroup;
				break;
			} 
		}
		
		this.cmdGroupToCmdClasses.computeIfAbsent(cmdGroupToUse, cmdGrp -> 
				new TreeSet<Class<?>>(new Comparator<Class<?>>() {
					@SuppressWarnings("unchecked")
					public int compare(Class<?> c1, Class<?> c2) {
						String id1 = String.join("_", CommandUsage.cmdWordsForCmdClass((Class<? extends Command>) c1));
						String id2 = String.join("_", CommandUsage.cmdWordsForCmdClass((Class<? extends Command>) c2));
						return id1.compareTo(id2);
					}
				})).add(cmdClass);
		this.cmdClassToCmdGroup.put(cmdClass, cmdGroupToUse);
	}
	
	@SuppressWarnings("rawtypes")
	public CommandGroup getCmdGroupForCmdClass(Class<? extends Command> cmdClass) {
		return this.cmdClassToCmdGroup.get(cmdClass);
	}
	
	public void setCmdGroup(CommandGroup cmdGroup) {
		this.cmdGroup = cmdGroup;
	}

	
	public Map<CommandGroup, TreeSet<Class<?>>> getCmdGroupToCmdClasses() {
		ArrayList<CommandGroup> cmdGroups = new ArrayList<CommandGroup>(cmdGroupToCmdClasses.keySet());
		Collections.sort(cmdGroups);
		Map<CommandGroup, TreeSet<Class<?>>> cmdGroupToCmdClassesSorted = new LinkedHashMap<CommandGroup, TreeSet<Class<?>>>();
		for(CommandGroup cmdGroup: cmdGroups) {
			cmdGroupToCmdClassesSorted.put(cmdGroup, cmdGroupToCmdClasses.get(cmdGroup));
		}
		return cmdGroupToCmdClassesSorted;
	}

	
	
	
}
