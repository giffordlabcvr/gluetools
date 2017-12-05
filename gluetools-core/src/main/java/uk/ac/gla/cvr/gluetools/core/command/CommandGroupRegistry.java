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
