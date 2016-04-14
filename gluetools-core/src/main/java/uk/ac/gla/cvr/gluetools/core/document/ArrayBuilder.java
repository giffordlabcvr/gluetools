package uk.ac.gla.cvr.gluetools.core.document;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import uk.ac.gla.cvr.gluetools.utils.GlueTypeUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public class ArrayBuilder {

	private Element parentElement;
	private String name;

	ArrayBuilder(Element parentElement, String name) {
		super();
		this.parentElement = parentElement;
		this.name = name;
	}
	
	public ArrayBuilder addInt(int value) {
		return add(Integer.toString(value), GlueTypeUtils.GlueType.Integer);
	}

	public ArrayBuilder addBoolean(boolean value) {
		return add(Boolean.toString(value), GlueTypeUtils.GlueType.Boolean);
	}

	public ArrayBuilder addDouble(double value) {
		return add(Double.toString(value), GlueTypeUtils.GlueType.Double);
	}

	public ArrayBuilder addNull() {
		Element elem = GlueXmlUtils.appendElement(parentElement, name);
		GlueTypeUtils.setGlueType(elem, GlueTypeUtils.GlueType.Null, true);
		return this;
	}

	public ObjectBuilder addObject() {
		Element elem = GlueXmlUtils.appendElement(parentElement, name);
		return new ObjectBuilder(elem, true);
	}

	public ArrayBuilder addString(String value) {
		return add(value, GlueTypeUtils.GlueType.String);
	}

	public ArrayBuilder add(Object value) {
		if(value == null) {
			return addNull();
		} else if(value instanceof Double) {
			return addDouble(((Double) value).doubleValue());
		} else if(value instanceof Float) {
			return addDouble(((Float) value).doubleValue());
		} else if(value instanceof Integer) {
			return addInt(((Integer) value).intValue());
		} else if(value instanceof Boolean) {
			return addBoolean(((Boolean) value).booleanValue());
		} else if(value instanceof String) {
			return addString(((String) value));
		} else {
			throw new RuntimeException("Object of type: "+value.getClass().getCanonicalName()+" cannot be put in a GLUE document");
		}
	}
	
	private ArrayBuilder add(String string, GlueTypeUtils.GlueType type) {
		Node textNode = GlueXmlUtils.appendElementWithText(parentElement, name, string);
		GlueTypeUtils.setGlueType((Element) textNode.getParentNode(), type, true);
		return this;
	}

	
}
