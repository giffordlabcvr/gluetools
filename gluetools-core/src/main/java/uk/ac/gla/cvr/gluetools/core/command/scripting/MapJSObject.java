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
package uk.ac.gla.cvr.gluetools.core.command.scripting;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import jdk.nashorn.api.scripting.AbstractJSObject;

public class MapJSObject extends AbstractJSObject {
	private Map<String, Object> map;
	
	public MapJSObject(Map<String, Object> map) {
		super();
		this.map = map;
	}

	public void put(String key, Object value) {
		map.put(key, value);
	}

	@Override
	public Object getMember(String name) {
		return map.get(name);
	}

	@Override
	public boolean hasMember(String name) {
		return map.containsKey(name);
	}

	@Override
	public void removeMember(String name) {
		map.remove(name);
	}

	@Override
	public void setMember(String name, Object value) {
		map.put(name, value);
	}

	@Override
	public Set<String> keySet() {
		return map.keySet();
	}

	@Override
	public Collection<Object> values() {
		return map.values();
	}
}