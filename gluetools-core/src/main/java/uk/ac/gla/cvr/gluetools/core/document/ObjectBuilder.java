package uk.ac.gla.cvr.gluetools.core.document;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import uk.ac.gla.cvr.gluetools.utils.GlueTypeUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public class ObjectBuilder {

	private Element parentElement;

	public ObjectBuilder(Element parentElement, boolean isArray) {
		super();
		this.parentElement = parentElement;
		GlueTypeUtils.setGlueType(parentElement, GlueTypeUtils.GlueType.Object, isArray);
	}

	Element getElement() {
		return parentElement;
	}
	
	public ObjectReader getObjectReader() {
		return new ObjectReader(getElement());
	}
	
	public ObjectBuilder setInt(String name, int value) {
		return setSimpleProperty(name, Integer.toString(value), GlueTypeUtils.GlueType.Integer);
	}

	public ObjectBuilder setBoolean(String name, boolean value) {
		return setSimpleProperty(name, Boolean.toString(value), GlueTypeUtils.GlueType.Boolean);
	}

	public ObjectBuilder setDouble(String name, double value) {
		return setSimpleProperty(name, Double.toString(value), GlueTypeUtils.GlueType.Double);
	}

	public ObjectBuilder setNull(String name) {
		Element elem = GlueXmlUtils.appendElement(getElement(), name);
		GlueTypeUtils.setGlueType(elem, GlueTypeUtils.GlueType.Null, false);
		return this;
	}

	public ObjectBuilder setString(String name, String value) {
		return setSimpleProperty(name, value, GlueTypeUtils.GlueType.String);
	}

	
	public ObjectBuilder set(String name, Object value) {
		if(value == null) {
			return setNull(name);
		} else if(value instanceof Double) {
			return setDouble(name, ((Double) value).doubleValue());
		} else if(value instanceof Integer) {
			return setInt(name, ((Integer) value).intValue());
		} else if(value instanceof Boolean) {
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

	
	private ObjectBuilder setSimpleProperty(String name, String string, GlueTypeUtils.GlueType type) {
		Node textNode = GlueXmlUtils.appendElementWithText(getElement(), name, string);
		GlueTypeUtils.setGlueType((Element) textNode.getParentNode(), type, false);
		return this;
	}

	public void setImportedDocument(Document document) {
		Element elem = getElement();
		Document ownerDocument = elem.getOwnerDocument();
		Element elemToImport = document.getDocumentElement();
		NodeList childNodes = elemToImport.getChildNodes();
		for(int i = 0; i < childNodes.getLength(); i++) {
			elem.appendChild(ownerDocument.importNode(childNodes.item(i), true));
		}
	}
	
	
	 
}
