package uk.ac.gla.cvr.gluetools.utils;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.JsonValue.ValueType;
import javax.json.stream.JsonGenerator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class JsonUtils {

	private static final String XML_TO_JSON_TYPE_PROPERTY = "uk.ac.gla.cvr.gluetools.xmlToJSonType";
	private static final String XML_TO_JSON_IS_ARRAY_PROPERTY = "uk.ac.gla.cvr.gluetools.xmlToJSonIsArray";
	
	public enum JsonType {
		Object, 
		Integer,
		Double,
		String,
		Boolean,
		Null
	}
	
	public static JsonObject newObject() {
		return jsonObjectBuilder().build();
	}

	public static JsonObjectBuilder jsonObjectBuilder() {
		return Json.createObjectBuilder();
	}

	public static JsonArrayBuilder jsonArrayBuilder() {
		return Json.createArrayBuilder();
	}

	public static JsonObjectBuilder documentToJSonObjectBuilder(Document document) {
		Element docElem = document.getDocumentElement();
		JsonObjectBuilder docBuilder = jsonObjectBuilder();
		elementToJSon(docBuilder, docElem);
		return docBuilder;
	}

	private static void elementToJSon(JsonObjectBuilder parentBuilder, Element element) {
		String elemName = element.getNodeName();
		JsonType jsonType = getJsonType(element);
		if(jsonType == null) { throw new RuntimeException("Element: "+elemName+" has no JSON type"); }
		switch(jsonType) {
		case Object:
			JsonObjectBuilder childBuilder = jsonObjectBuilder();
			childElemsToJson(childBuilder, element);
			parentBuilder.add(elemName, childBuilder);
			break;
		case Double:
			parentBuilder.add(elemName, Double.parseDouble(element.getTextContent()));
			break;
		case Integer:
			parentBuilder.add(elemName, Integer.parseInt(element.getTextContent()));
			break;
		case Boolean:
			parentBuilder.add(elemName, Boolean.parseBoolean(element.getTextContent()));
			break;
		case String:
			parentBuilder.add(elemName, element.getTextContent());
			break;
		case Null:
			parentBuilder.addNull(elemName);
			break;
		}
	}

	private static void childElemsToJson(JsonObjectBuilder childBuilder, Element element) {
		List<Element> childElements = XmlUtils.findChildElements(element);
		Map<String, List<Element>> arrayItems = new LinkedHashMap<String, List<Element>>();
		childElements.forEach(childElem -> {
			String childElemName = childElem.getNodeName();
			Boolean isJsonArray = isJsonArray(childElem);
			if(isJsonArray) {
				List<Element> arrayElems = arrayItems.getOrDefault(childElemName, new ArrayList<Element>());
				arrayElems.add(childElem);
				arrayItems.putIfAbsent(childElemName, arrayElems);
			} else {
				elementToJSon(childBuilder, childElem);
			}
		});
		arrayItems.forEach((name, arrayElems) -> {
			JsonArrayBuilder jsonArrayBuilder = jsonArrayBuilder();
			for(Element arrayElem: arrayElems) {
				elementToJSon(jsonArrayBuilder, arrayElem);
			}
			childBuilder.add(name, jsonArrayBuilder);
		});
	}
	
	private static void elementToJSon(JsonArrayBuilder parentBuilder, Element element) {
		JsonType jsonType = getJsonType(element);
		if(jsonType == null) { throw new RuntimeException("Element: "+element.getNodeName()+" has no JSON type"); }
		switch(jsonType) {
		case Object:
			JsonObjectBuilder childBuilder = jsonObjectBuilder();
			childElemsToJson(childBuilder, element);
			parentBuilder.add(childBuilder);
			break;
		case Double:
			parentBuilder.add(Double.parseDouble(element.getTextContent()));
			break;
		case Integer:
			parentBuilder.add(Integer.parseInt(element.getTextContent()));
			break;
		case Boolean:
			parentBuilder.add(Boolean.parseBoolean(element.getTextContent()));
			break;
		case String:
			parentBuilder.add(element.getTextContent());
			break;
		case Null:
			parentBuilder.addNull();
			break;
		}
	}
	
	public static String prettyPrint(JsonObject jsonObject) {
		Map<String, Boolean> config = Collections.singletonMap(JsonGenerator.PRETTY_PRINTING, true);
        JsonWriterFactory jwf = Json.createWriterFactory(config);
        StringWriter sw = new StringWriter();
        try (JsonWriter jsonWriter = jwf.createWriter(sw)) {
            jsonWriter.writeObject(jsonObject);
        }
        return sw.toString();
   }

	public static void setJsonType(Element elem, JsonType jsonType, boolean isArray) {
		elem.setUserData(XML_TO_JSON_TYPE_PROPERTY, jsonType, null);
		elem.setUserData(XML_TO_JSON_IS_ARRAY_PROPERTY, isArray, null);
	}

	public static JsonType getJsonType(Element elem) {
		Object userData = elem.getUserData(XML_TO_JSON_TYPE_PROPERTY);
		if(userData == null) {
			throw new RuntimeException("Element "+elem.getNodeName()+" has no JSON type.");
		}
		return (JsonType) userData;
	}

	public static boolean isJsonArray(Element elem) {
		Object userData = elem.getUserData(XML_TO_JSON_IS_ARRAY_PROPERTY);
		if(userData == null) {
			throw new RuntimeException("Element "+elem.getNodeName()+" has no JSON array boolean.");
		}
		return (Boolean) userData;
	}

	public static JsonObject stringToJsonObject(String string) {
		return Json.createReader(new StringReader(string)).readObject();
	}

	public static void jsonObjectToElement(Element parentElem, JsonObject jsonObject) {
		jsonObject.forEach((key, value) -> {
			jsonValueToElement(parentElem, key, value);
		});
	}

	private static void jsonValueToElement(Element parentElem, String key, JsonValue value) {
		if(value instanceof JsonObject) {
			Element childElem = XmlUtils.appendElement(parentElem, key);
			jsonObjectToElement(childElem, (JsonObject) value);
		} else if(value instanceof JsonArray) {
			((JsonArray) value).forEach(item -> {
				jsonValueToElement(parentElem, key, item);
			});
		} else if(value.getValueType() == ValueType.NULL){
			Element elem = XmlUtils.appendElement(parentElem, key);
			elem.setAttribute("isNull", "true");
		} else {
			XmlUtils.appendElementWithText(parentElem, key, value.toString());
		}
	}
	
	
}
