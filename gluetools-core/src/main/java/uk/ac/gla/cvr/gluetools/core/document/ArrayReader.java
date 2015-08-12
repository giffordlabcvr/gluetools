package uk.ac.gla.cvr.gluetools.core.document;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.utils.JsonUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils.JsonType;

public class ArrayReader {

	private List<Element> elements;

	public ArrayReader(List<Element> elements) {
		super();
		this.elements = elements;
	}
	
	public JsonType getItemType(int i) {
		return JsonUtils.getJsonType(getItemElement(i));
	}

	private Element getItemElement(int i) {
		return elements.get(i);
	}
	
	public Object value(int i) {
		return JsonUtils.elementToObject(getItemElement(i));
	}
	
	public Boolean booleanValue(int i) {
		return (Boolean) value(i);
	}

	public Integer intValue(int i) {
		return (Integer) value(i);
	}

	public String stringValue(int i) {
		return (String) value(i);
	}

	public Double doubleValue(int i) {
		return (Double) value(i);
	}

	public ObjectReader getObject(int i) {
		return new ObjectReader(getItemElement(i));
	}

	public int size() {
		return elements.size();
	}

	
}
