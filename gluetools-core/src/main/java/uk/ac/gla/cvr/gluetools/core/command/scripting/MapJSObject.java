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