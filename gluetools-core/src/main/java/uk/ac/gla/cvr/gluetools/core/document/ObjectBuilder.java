package uk.ac.gla.cvr.gluetools.core.document;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils.JsonType;

public class ObjectBuilder {

	private Element element;

	ObjectBuilder(Element element, boolean isArray) {
		super();
		this.element = element;
		JsonUtils.setJsonType(element, JsonType.Object, isArray);
	}

	Element getElement() {
		return element;
	}
	
	public void setInt(String name, int value) {
		setSimpleProperty(name, Integer.toString(value), JsonType.Integer);
	}

	public void setBoolean(String name, boolean value) {
		setSimpleProperty(name, Boolean.toString(value), JsonType.Boolean);
	}

	public void setDouble(String name, double value) {
		setSimpleProperty(name, Double.toString(value), JsonType.Double);
	}

	public void setNull(String name) {
		Element elem = GlueXmlUtils.appendElement(getElement(), name);
		JsonUtils.setJsonType(elem, JsonType.Null, false);
	}

	public void set(String name, String value) {
		setSimpleProperty(name, value, JsonType.String);
	}

	public ObjectBuilder setObject(String name) {
		Element elem = GlueXmlUtils.appendElement(getElement(), name);
		return new ObjectBuilder(elem, false);
	}

	public ArrayBuilder setArray(String name) {
		return new ArrayBuilder(getElement(), name);
	}

	
	private void setSimpleProperty(String name, String string, JsonType type) {
		Node textNode = GlueXmlUtils.appendElementWithText(getElement(), name, string);
		JsonUtils.setJsonType((Element) textNode.getParentNode(), type, false);
	}

	
	
	
}
