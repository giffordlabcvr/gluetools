package uk.ac.gla.cvr.gluetools.core.document;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils.JsonType;

public class ArrayBuilder {

	private Element parentElement;
	private String name;

	ArrayBuilder(Element parentElement, String name) {
		super();
		this.parentElement = parentElement;
		this.name = name;
	}
	
	public ArrayBuilder addInt(int value) {
		return add(Integer.toString(value), JsonType.Integer);
	}

	public ArrayBuilder addBoolean(boolean value) {
		return add(Boolean.toString(value), JsonType.Boolean);
	}

	public ArrayBuilder addDouble(double value) {
		return add(Double.toString(value), JsonType.Double);
	}

	public ArrayBuilder addNull() {
		Element elem = GlueXmlUtils.appendElement(parentElement, name);
		JsonUtils.setJsonType(elem, JsonType.Null, true);
		return this;
	}

	public ObjectBuilder addObject() {
		Element elem = GlueXmlUtils.appendElement(parentElement, name);
		return new ObjectBuilder(elem, true);
	}

	public ArrayBuilder addString(String value) {
		return add(value, JsonType.String);
	}

	public ArrayBuilder add(Object value) {
		if(value == null) {
			return addNull();
		} else if(value instanceof Double) {
			return addDouble(((Double) value).doubleValue());
		} else if(value instanceof Integer) {
			return addInt(((Integer) value).intValue());
		} else if(value instanceof Double) {
			return addBoolean(((Boolean) value).booleanValue());
		} else if(value instanceof String) {
			return addString(((String) value));
		} else {
			throw new RuntimeException("Object of type: "+value.getClass().getCanonicalName()+" cannot be put in a GLUE document");
		}
	}
	
	private ArrayBuilder add(String string, JsonType type) {
		Node textNode = GlueXmlUtils.appendElementWithText(parentElement, name, string);
		JsonUtils.setJsonType((Element) textNode.getParentNode(), type, true);
		return this;
	}

	
	
}
