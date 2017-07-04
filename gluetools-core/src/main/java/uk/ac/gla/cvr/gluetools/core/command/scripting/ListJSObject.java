package uk.ac.gla.cvr.gluetools.core.command.scripting;

import java.util.List;

import jdk.nashorn.api.scripting.AbstractJSObject;

public class ListJSObject extends AbstractJSObject {

	private List<Object> list;
	
	public ListJSObject(List<Object> list) {
		super();
		this.list = list;
	}

	public void add(Object value) {
		list.add(value);
	}

	@Override
	public Object getSlot(int index) {
		return list.get(index);
	}

	@Override
	public boolean hasSlot(int slot) {
		return slot >= 0 && slot < list.size();
	}

	@Override
	public void setSlot(int index, Object value) {
		list.set(index, value);
	}

	@Override
	public boolean isArray() {
		return true;
	}
	
}