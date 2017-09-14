package uk.ac.gla.cvr.gluetools.utils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.CommandFieldValue;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentException.Code;
import uk.ac.gla.cvr.gluetools.utils.GlueTypeUtils.GlueType;

public class CommandDocumentXmlUtils {

	
	public static Document commandDocumentToXmlDocument(CommandDocument commandDocument) {
		CommandDocumentToXmlDocumentVisitor commandDocumentToXmlDocumentVisitor = new CommandDocumentToXmlDocumentVisitor();
		commandDocument.accept(commandDocumentToXmlDocumentVisitor);
		return commandDocumentToXmlDocumentVisitor.getXmlDocument();
	}
	
	public static CommandDocument xmlDocumentToCommandDocument(Document xmlDocument) {
		Element docRootElem = xmlDocument.getDocumentElement();
		CommandDocument commandDocument = new CommandDocument(docRootElem.getNodeName());
		populateCommandObjectFromElement(commandDocument, docRootElem);
		return commandDocument;
	}

	
	
	public static void populateCommandObjectFromElement(CommandObject commandObject, Element element) {
		GlueXmlUtils.findChildElements(element)
			.forEach(childElem -> populateCommandObjectFieldFromChildElement(commandObject, childElem));;
	}



	private static void populateCommandObjectFieldFromChildElement(CommandObject commandObject, Element childElem) {
		String fieldName = childElem.getNodeName();
		GlueType childGlueType = getGlueType(childElem);
		boolean isMemberOfArray = isMemberOfArray(childElem);
		CommandFieldValue currentFieldValue = commandObject.getFieldValue(fieldName);
		if(isMemberOfArray) {
			CommandArray commandArray;
			if(currentFieldValue == null) {
				commandArray = commandObject.setArray(fieldName);
			} else if(currentFieldValue instanceof CommandArray) {
				commandArray = (CommandArray) currentFieldValue;
			} else {
				throw new CommandDocumentException(Code.XML_TO_COMMAND_DOCUMENT_ERROR, "Cannot set GLUE object field "+fieldName+" as array, it already has a non-array type");
			}
			populateCommandArrayItemFromChildElement(commandArray, childElem);
		} else {
			if(currentFieldValue != null) {
				throw new CommandDocumentException(Code.XML_TO_COMMAND_DOCUMENT_ERROR, "Cannot set value twice for "+fieldName);
			}
			if(childGlueType == GlueType.Object) {
				populateCommandObjectFromElement(commandObject.setObject(fieldName), childElem);
			} else {
				// simple value
				commandObject.set(fieldName, GlueTypeUtils.typeStringToObject(childGlueType, childElem.getTextContent()));
			}
		}
	}

	private static void populateCommandArrayItemFromChildElement(CommandArray commandArray, Element childElem) {
		GlueType childGlueType = getGlueType(childElem);
		if(childGlueType == GlueType.Object) {
			populateCommandObjectFromElement(commandArray.addObject(), childElem);
		} else {
			// simple value
			commandArray.add(GlueTypeUtils.typeStringToObject(childGlueType, childElem.getTextContent()));
		}
		
	}



	public static final String GLUE_TYPE_ATTRIBUTE = "glueType";
	public static void setGlueType(Element elem, GlueType glueType, boolean isMemberOfArray) {
		if(isMemberOfArray) {
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
			throw new CommandDocumentException(Code.XML_TO_COMMAND_DOCUMENT_ERROR, "Attribute value "+typeString+" is not a valid GLUE type.");
		}
	}

	public static boolean isMemberOfArray(Element elem) {
		String typeString = elem.getAttribute(GLUE_TYPE_ATTRIBUTE);
		if(typeString == null) {
			throw new CommandDocumentException(Code.XML_TO_COMMAND_DOCUMENT_ERROR, "Element "+elem.getNodeName()+" has no GLUE type attribute.");
		}
		return typeString.endsWith("[]");
	}

}
