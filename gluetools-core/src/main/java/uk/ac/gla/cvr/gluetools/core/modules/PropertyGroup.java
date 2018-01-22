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
package uk.ac.gla.cvr.gluetools.core.modules;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PropertyGroup {
	private List<String> propertyNames = new ArrayList<String>();
	private Map<String, PropertyGroup> children = new LinkedHashMap<String, PropertyGroup>();

	public PropertyGroup addPropertyName(String simplePropertyName) {
		this.propertyNames.add(simplePropertyName);
		return this;
	}
	
	public List<String> allPropertyPaths() {
		List<String> paths = new ArrayList<String>();
		paths.addAll(propertyNames);
		children.forEach((groupName, group) -> {
			paths.addAll(group.allPropertyPaths().stream().map(s -> groupName+"/"+s).collect(Collectors.toList()));
		});
		return paths;
	}

	public List<String> allPropertyGroupPaths() {
		List<String> paths = new ArrayList<String>();
		children.forEach((groupName, group) -> {
			paths.add(groupName);
			paths.addAll(group.allPropertyGroupPaths().stream().map(s -> groupName+"/"+s).collect(Collectors.toList()));
		});
		return paths;
	}

	public List<String> getPropertyNames() {
		return propertyNames;
	}

	public PropertyGroup addChild(String groupName) {
		PropertyGroup propertyGroup = new PropertyGroup();
		children.put(groupName, propertyGroup);
		return propertyGroup;
	}

	public PropertyGroup getChild(String groupName) {
		return children.get(groupName);
	}

	public boolean validProperty(List<String> propertyPathElems) {
		if(propertyPathElems.size() == 0) {
			return false;
		}
		if(propertyPathElems.size() == 1) {
			return propertyNames.contains(propertyPathElems.get(0));
		}
		PropertyGroup child = getChild(propertyPathElems.get(0));
		if(child == null) {
			return false;
		}
		return child.validProperty(propertyPathElems.subList(1, propertyPathElems.size()));
	}

	public boolean validPropertyGroup(List<String> propertyPathElems) {
		if(propertyPathElems.size() == 0) {
			return false;
		}
		if(propertyPathElems.size() == 1) {
			return children.keySet().contains(propertyPathElems.get(0));
		}
		PropertyGroup child = getChild(propertyPathElems.get(0));
		if(child == null) {
			return false;
		}
		return child.validPropertyGroup(propertyPathElems.subList(1, propertyPathElems.size()));
	}

}