package uk.ac.gla.cvr.gluetools.core.plugins;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeSet;

public class PluginGroupRegistry<G extends PluginGroup, P extends Plugin> {

	private Map<G, TreeSet<String>> pluginGroupToElemNames = new LinkedHashMap<G, TreeSet<String>>();

	private Map<String, G> elemNameToPluginGroup = new LinkedHashMap<String, G>();

	
	private G defaultGroup;
	
	public PluginGroupRegistry(G defaultGroup) {
		super();
		this.defaultGroup = defaultGroup;
	}

	// set this before registering a set of plugin classes.
	// these plugins will be added to the relevant group
	// for documentation purposes.
	private G pluginGroup = null;

	@SuppressWarnings("rawtypes")
	public void registerElemName(String elemName) {
		G pluginGroupToUse = this.pluginGroup;
		if(pluginGroupToUse == null) {
			pluginGroupToUse = defaultGroup;
		}
		
		this.pluginGroupToElemNames.computeIfAbsent(pluginGroupToUse, cmdGrp -> 
				new TreeSet<String>()).add(elemName);
		this.elemNameToPluginGroup.put(elemName, pluginGroupToUse);
	}
	
	@SuppressWarnings("rawtypes")
	public G getPluginGroupForElemName(String elemName) {
		return this.elemNameToPluginGroup.get(elemName);
	}
	
	public void setPluginGroup(G cmdGroup) {
		this.pluginGroup = cmdGroup;
	}

	
	public Map<G, TreeSet<String>> getPluginGroupToElemNames() {
		ArrayList<G> cmdGroups = new ArrayList<G>(pluginGroupToElemNames.keySet());
		Collections.sort(cmdGroups);
		Map<G, TreeSet<String>> cmdGroupToElemNamesSorted = new LinkedHashMap<G, TreeSet<String>>();
		for(G cmdGroup: cmdGroups) {
			cmdGroupToElemNamesSorted.put(cmdGroup, pluginGroupToElemNames.get(cmdGroup));
		}
		return cmdGroupToElemNamesSorted;
	}

	
	
	
}
