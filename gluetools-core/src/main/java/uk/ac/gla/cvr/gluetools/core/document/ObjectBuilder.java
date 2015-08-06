package uk.ac.gla.cvr.gluetools.core.document;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils.JsonType;

public class ObjectBuilder {

	private Element parentElement;

	ObjectBuilder(Element parentElement, boolean isArray) {
		super();
		this.parentElement = parentElement;
		JsonUtils.setJsonType(parentElement, JsonType.Object, isArray);
	}

	Element getElement() {
		return parentElement;
	}
	
	public ObjectBuilder setInt(String name, int value) {
		return setSimpleProperty(name, Integer.toString(value), JsonType.Integer);
	}

	public ObjectBuilder setBoolean(String name, boolean value) {
		return setSimpleProperty(name, Boolean.toString(value), JsonType.Boolean);
	}

	public ObjectBuilder setDouble(String name, double value) {
		return setSimpleProperty(name, Double.toString(value), JsonType.Double);
	}

	public ObjectBuilder setNull(String name) {
		Element elem = GlueXmlUtils.appendElement(getElement(), name);
		JsonUtils.setJsonType(elem, JsonType.Null, false);
		return this;
	}

	public ObjectBuilder setString(String name, String value) {
		return setSimpleProperty(name, value, JsonType.String);
	}

	
	public ObjectBuilder set(String name, Object value) {
		if(value == null) {
			return setNull(name);
		} else if(value instanceof Double) {
			return setDouble(name, ((Double) value).doubleValue());
		} else if(value instanceof Integer) {
			return setInt(name, ((Integer) value).intValue());
		} else if(value instanceof Double) {
			return setBoolean(name, ((Boolean) value).booleanValue());
		} else if(value instanceof String) {
			return setString(name, ((String) value));
		} else {
			throw new RuntimeException("Object of type: "+value.getClass().getCanonicalName()+" cannot be put in a GLUE document");
		}
	}

	
	public ObjectBuilder setObject(String name) {
		Element elem = GlueXmlUtils.appendElement(getElement(), name);
		return new ObjectBuilder(elem, false);
	}

	public ArrayBuilder setArray(String name) {
		return new ArrayBuilder(getElement(), name);
	}

	
	private ObjectBuilder setSimpleProperty(String name, String string, JsonType type) {
		Node textNode = GlueXmlUtils.appendElementWithText(getElement(), name, string);
		JsonUtils.setJsonType((Element) textNode.getParentNode(), type, false);
		return this;
	}

	
	
	
}
