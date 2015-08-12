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
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import javax.json.stream.JsonGenerator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class JsonUtils {

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
		GlueTypeUtils.GlueType glueType = GlueTypeUtils.getGlueType(element);
		if(glueType == null) { throw new RuntimeException("Element: "+elemName+" has no GLUE type"); }
		switch(glueType) {
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
		List<Element> childElements = GlueXmlUtils.findChildElements(element);
		Map<String, List<Element>> arrayItems = new LinkedHashMap<String, List<Element>>();
		childElements.forEach(childElem -> {
			String childElemName = childElem.getNodeName();
			Boolean isJsonArray = GlueTypeUtils.isGlueArray(childElem);
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
		GlueTypeUtils.GlueType glueType = GlueTypeUtils.getGlueType(element);
		if(glueType == null) { throw new RuntimeException("Element: "+element.getNodeName()+" has no JSON type"); }
		switch(glueType) {
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
			Element childElem = GlueXmlUtils.appendElement(parentElem, key);
			jsonObjectToElement(childElem, (JsonObject) value);
		} else if(value instanceof JsonArray) {
			((JsonArray) value).forEach(item -> {
				jsonValueToElement(parentElem, key, item);
			});
		} else if(value.getValueType() == ValueType.NULL){
			Element elem = GlueXmlUtils.appendElement(parentElem, key);
			elem.setAttribute("isNull", "true");
		} else if(value instanceof JsonString) {
			GlueXmlUtils.appendElementWithText(parentElem, key, ((JsonString) value).getChars().toString());
		} else if(value instanceof JsonNumber) {
			GlueXmlUtils.appendElementWithText(parentElem, key, ((JsonNumber) value).toString());
		} else {
			GlueXmlUtils.appendElementWithText(parentElem, key, value.toString());
		}
	}
	
}
