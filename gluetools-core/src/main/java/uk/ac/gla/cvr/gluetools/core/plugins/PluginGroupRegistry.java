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
