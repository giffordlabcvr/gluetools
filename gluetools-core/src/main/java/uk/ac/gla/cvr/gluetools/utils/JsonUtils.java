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

import uk.ac.gla.cvr.gluetools.core.document.ArrayBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;


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
		case Date:
			parentBuilder.add(elemName, DateUtils.render(DateUtils.parse(element.getTextContent())));
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
		case Date:
			parentBuilder.add(element.getTextContent());
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

	public static void buildObjectFromJson(ObjectBuilder objectBuilder, JsonObject jsonObject) {
		jsonObject.forEach((key, value) -> {
			buildChildObjectFromJson(objectBuilder, key, value);
		});
	}

	private static void buildChildObjectFromJson(ObjectBuilder parentObjBuilder, String key, JsonValue value) {
		if(value instanceof JsonObject) {
			ObjectBuilder childObjBuilder = parentObjBuilder.setObject(key);
			buildObjectFromJson(childObjBuilder, (JsonObject) value);
		} else if(value instanceof JsonArray) {
			ArrayBuilder arrayBuilder = parentObjBuilder.setArray(key);
			((JsonArray) value).forEach(item -> {
				buildArrayItemFromJson(arrayBuilder, item);
			});
		} else if(value.getValueType() == ValueType.NULL){
			parentObjBuilder.setNull(key);
		} else if(value instanceof JsonString) {
			String string = ((JsonString) value).getChars().toString();
			if(DateUtils.isDateString(string)) {
				parentObjBuilder.setDate(key, DateUtils.parse(string));
			} else {
				parentObjBuilder.setString(key, string);
			}
		} else if(value.toString().equals("true") || value.toString().equals("false")) {
			parentObjBuilder.setBoolean(key, Boolean.parseBoolean(value.toString()));
		} else if(value instanceof JsonNumber) {
			JsonNumber jsonNumber = (JsonNumber) value;
			if(jsonNumber.isIntegral()) {
				parentObjBuilder.setInt(key, jsonNumber.intValue());
			} else {
				parentObjBuilder.setDouble(key, jsonNumber.doubleValue());
			}
		} else {
			throw new RuntimeException("Unable to handle JsonValue: "+value);
		}
	}

	private static void buildArrayItemFromJson(ArrayBuilder arrayBuilder, JsonValue item) {
		if(item instanceof JsonObject) {
			ObjectBuilder childObjBuilder = arrayBuilder.addObject();
			buildObjectFromJson(childObjBuilder, (JsonObject) item);
		} else if(item instanceof JsonArray) {
			throw new RuntimeException("Unable to handle Json arrays within arrays");
		} else if(item.getValueType() == ValueType.NULL){
			arrayBuilder.addNull();
		} else if(item instanceof JsonString) {
			arrayBuilder.addString(((JsonString) item).getChars().toString());
		} else if(item.toString().equals("true") || item.toString().equals("false")) {
			arrayBuilder.addBoolean(Boolean.parseBoolean(item.toString()));
		} else if(item instanceof JsonNumber) {
			JsonNumber jsonNumber = (JsonNumber) item;
			if(jsonNumber.isIntegral()) {
				arrayBuilder.addInt(jsonNumber.intValue());
			} else {
				arrayBuilder.addDouble(jsonNumber.doubleValue());
			}
		} else {
			throw new RuntimeException("Unable to handle JsonValue: "+item);
		}
	}

	public static void generateJsonFromDocument(JsonGenerator jsonGenerator, Document document) {
		Element docElem = document.getDocumentElement();
		JsonObjectBuilder docBuilder = jsonObjectBuilder();
		elementToJSon(docBuilder, docElem);
	}
	
}
