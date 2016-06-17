package uk.ac.gla.cvr.gluetools.core.document;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.utils.GlueTypeUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public class ObjectReader {

	private Element element;

	public ObjectReader(Element element) {
		super();
		GlueTypeUtils.GlueType glueType = GlueTypeUtils.getGlueType(element);
		if(glueType != GlueTypeUtils.GlueType.Object) {
			throw new RuntimeException("Element is not of type Object");
		}
		this.element = element;
	}

	public List<String> getFieldNames() {
		Set<String> fieldNamesSet = new LinkedHashSet<String>();
		for(Element childElem: GlueXmlUtils.findChildElements(element)) {
			fieldNamesSet.add(childElem.getNodeName());
		}
		return new LinkedList<String>(fieldNamesSet);
	}
	
	public GlueTypeUtils.GlueType getFieldType(String name) {
		return GlueTypeUtils.getGlueType(getFieldElement(name));
	}

	private Element getFieldElement(String name) {
		List<Element> childElems = GlueXmlUtils.findChildElements(element, name);
		if(childElems.size() == 0) {
			return null;
		}
		if(childElems.size() == 1) {
			Element childElem = childElems.get(0);
			if(GlueTypeUtils.isGlueArray(childElem)) {
				throw new RuntimeException("Field "+name+" is an array rather than an Object");
			}
			return childElem;
		} else {
			throw new RuntimeException("Field "+name+" has multiple values");
		}
	}
	
	public String getName() {
		return element.getNodeName();
	}
	
	public Object value(String name) {
		Element fieldElement = getFieldElement(name);
		if(fieldElement == null) {
			return null;
		}
		return GlueTypeUtils.elementToObject(fieldElement);
	}
	
	public Boolean booleanValue(String name) {
		return (Boolean) value(name);
	}

	public Date dateValue(String name) {
		return (Date) value(name);
	}

	public Integer intValue(String name) {
		return (Integer) value(name);
	}

	public String stringValue(String name) {
		return (String) value(name);
	}

	public Double doubleValue(String name) {
		return (Double) value(name);
	}

	public ArrayReader getArray(String name) {
		List<Element> childElems = GlueXmlUtils.findChildElements(element, name);
		for(Element childElem : childElems) {
			if(!GlueTypeUtils.isGlueArray(childElem)) {
				throw new RuntimeException("Field "+name+" is not an array");
			}
		}
		return new ArrayReader(childElems);
	}
	
	public ObjectReader getObject(String name) {
		return new ObjectReader(getFieldElement(name));
	}
	
}
