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
	
	public void addInt(int value) {
		add(Integer.toString(value), JsonType.Integer);
	}

	public void addBoolean(String name, boolean value) {
		add(Boolean.toString(value), JsonType.Boolean);
	}

	public void addDouble(String name, double value) {
		add(Double.toString(value), JsonType.Double);
	}

	public void addNull(String name) {
		Element elem = GlueXmlUtils.appendElement(parentElement, name);
		JsonUtils.setJsonType(elem, JsonType.Null, true);
	}

	public ObjectBuilder addObject(String name) {
		Element elem = GlueXmlUtils.appendElement(parentElement, name);
		return new ObjectBuilder(elem, true);
	}

	public void add(String name, String value) {
		add(value, JsonType.String);
	}

	private void add(String string, JsonType type) {
		Node textNode = GlueXmlUtils.appendElementWithText(parentElement, name, string);
		JsonUtils.setJsonType((Element) textNode.getParentNode(), type, true);
	}

	
	
}
