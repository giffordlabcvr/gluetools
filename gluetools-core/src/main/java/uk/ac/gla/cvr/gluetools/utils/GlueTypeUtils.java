package uk.ac.gla.cvr.gluetools.utils;

import java.util.Date;

import org.w3c.dom.Element;

public class GlueTypeUtils {

	public enum GlueType {
		Object, 
		Integer,
		Double,
		String,
		Boolean,
		Date,
		Null
	}

	public static Object elementToObject(Element elem) {
		GlueType glueType = GlueTypeUtils.getGlueType(elem);
		switch(glueType) {
		case Double:
			return Double.parseDouble(elem.getTextContent());
		case Integer:
			return Integer.parseInt(elem.getTextContent());
		case Boolean:
			return Boolean.parseBoolean(elem.getTextContent());
		case Date:
			return DateUtils.parse(elem.getTextContent());
		case String:
			return elem.getTextContent();
		case Null:
			return null;
		default:
			// maybe it could be a map?
			throw new RuntimeException("Element "+elem.getNodeName()+" cannot be cast to an object");
		}
	}

	public static GlueType glueTypeFromObject(Object value) {
		if(value == null) {
			return GlueType.Null;
		} else if (value instanceof Double) {
			return GlueType.Double;
		} else if (value instanceof Integer) {
			return GlueType.Integer;
		} else if (value instanceof Boolean) {
			return GlueType.Boolean;
		}  else if (value instanceof Date) {
			return GlueType.Date;
		} else if (value instanceof String) {
			return GlueType.String;
		} else {
			throw new RuntimeException("Object "+value+" is not a GLUE type");
		}
	}

	public static final String GLUE_TYPE_ATTRIBUTE = "glueType";
	public static void setGlueType(Element elem, GlueType glueType, boolean isArray) {
		if(isArray) {
			elem.setAttribute(GLUE_TYPE_ATTRIBUTE, glueType.name()+"[]");
		} else {
			elem.setAttribute(GLUE_TYPE_ATTRIBUTE, glueType.name());
		}
	}

	public static GlueType getGlueType(Element elem) {
		String typeString = elem.getAttribute(GLUE_TYPE_ATTRIBUTE);
		if(typeString.length() == 0) {
			if(GlueXmlUtils.findChildElements(elem).isEmpty()) {
				typeString = GlueType.String.name();
			} else {
				typeString = GlueType.Object.name();
			}
		}
		typeString = typeString.replace("[]", "");
		try {
			return GlueType.valueOf(typeString);
		} catch(IllegalArgumentException iae) {
			throw new RuntimeException("Attribute value "+typeString+" is not a valid GLUE type.");
		}
	}

	public static boolean isGlueArray(Element elem) {
		String typeString = elem.getAttribute(GLUE_TYPE_ATTRIBUTE);
		if(typeString == null) {
			throw new RuntimeException("Element "+elem.getNodeName()+" has no GLUE type attribute.");
		}
		return typeString.endsWith("[]");
	}

}
