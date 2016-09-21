package uk.ac.gla.cvr.gluetools.utils;

import java.util.ArrayList;
import java.util.List;

import javax.json.JsonArray;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import javax.json.JsonValue.ValueType;
import javax.json.stream.JsonGenerator;

import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentException.Code;

public class CommandDocumentJsonUtils {

	public static JsonObject commandDocumentToJsonObject(CommandDocument commandDocument) {
		JsonObjectCmdDocVisitor jsonObjectCmdDocVisitor = new JsonObjectCmdDocVisitor();
		commandDocument.accept(jsonObjectCmdDocVisitor);
		return jsonObjectCmdDocVisitor.getJsonObject();
	}
	
	public static void commandDocumentGenerateJson(JsonGenerator jsonGenerator, CommandDocument commandDocument	) {
		JsonGeneratorCmdDocVisitor jsonGeneratorCmdDocVisitor = new JsonGeneratorCmdDocVisitor(jsonGenerator);
		commandDocument.accept(jsonGeneratorCmdDocVisitor);
	}

	
	public static CommandDocument jsonObjectToCommandDocument(JsonObject jsonObject) {
		List<String> jsonKeySet = new ArrayList<String>(jsonObject.keySet());
		if(jsonKeySet.size() != 1) {
			throw new CommandDocumentException(Code.JSON_TO_COMMAND_DOCUMENT_ERROR, "Root JSON object must have a single key");
		}
		String rootName = jsonKeySet.get(0);
		CommandDocument documentBuilder = new CommandDocument(rootName);
		JsonValue jsonValue = jsonObject.get(rootName);
		if(!(jsonValue instanceof JsonObject)) {
			throw new CommandDocumentException(Code.JSON_TO_COMMAND_DOCUMENT_ERROR, "Root JSON object must have an object as its single value");
		}
		populateCommandObjectFromJsonObject(documentBuilder, (JsonObject) jsonValue);
		return documentBuilder;
	}

	
	private static void populateChildCommandObjectFromJsonValue(CommandObject parentCommandObject, String key, JsonValue jsonValue) {
		if(jsonValue instanceof JsonObject) {
			CommandObject childCommandObject = parentCommandObject.setObject(key);
			populateCommandObjectFromJsonObject(childCommandObject, (JsonObject) jsonValue);
		} else if(jsonValue instanceof JsonArray) {
			CommandArray arrayBuilder = parentCommandObject.setArray(key);
			((JsonArray) jsonValue).forEach(item -> {
				addArrayItemFromJsonValue(arrayBuilder, item);
			});
		} else if(jsonValue.getValueType() == ValueType.NULL){
			parentCommandObject.setNull(key);
		} else if(jsonValue instanceof JsonString) {
			String string = ((JsonString) jsonValue).getChars().toString();
			if(DateUtils.isDateString(string)) {
				parentCommandObject.setDate(key, DateUtils.parse(string));
			} else {
				parentCommandObject.setString(key, string);
			}
		} else if(jsonValue.getValueType() == ValueType.TRUE) {
			parentCommandObject.setBoolean(key, Boolean.TRUE);
		} else if(jsonValue.getValueType() == ValueType.FALSE) {
			parentCommandObject.setBoolean(key, Boolean.FALSE);
		} else if(jsonValue instanceof JsonNumber) {
			JsonNumber jsonNumber = (JsonNumber) jsonValue;
			if(jsonNumber.isIntegral()) {
				parentCommandObject.setInt(key, jsonNumber.intValue());
			} else {
				parentCommandObject.setDouble(key, jsonNumber.doubleValue());
			}
		} else {
			throw new CommandDocumentException(Code.JSON_TO_COMMAND_DOCUMENT_ERROR, "GLUE object may not contain JSON value "+jsonValue);
		}
	}

	private static void addArrayItemFromJsonValue(CommandArray commandArray, JsonValue jsonValue) {
		if(jsonValue instanceof JsonObject) {
			CommandObject childCommandObject = commandArray.addObject();
			populateCommandObjectFromJsonObject(childCommandObject, (JsonObject) jsonValue);
		} else if(jsonValue instanceof JsonArray) {
			throw new CommandDocumentException(Code.JSON_TO_COMMAND_DOCUMENT_ERROR, "GLUE array may not contain JSON array as item");
		} else if(jsonValue.getValueType() == ValueType.NULL){
			commandArray.addNull();
		} else if(jsonValue instanceof JsonString) {
			commandArray.addString(((JsonString) jsonValue).getChars().toString());
		} else if(jsonValue.toString().equals("true") || jsonValue.toString().equals("false")) {
			commandArray.addBoolean(Boolean.parseBoolean(jsonValue.toString()));
		} else if(jsonValue instanceof JsonNumber) {
			JsonNumber jsonNumber = (JsonNumber) jsonValue;
			if(jsonNumber.isIntegral()) {
				commandArray.addInt(jsonNumber.intValue());
			} else {
				commandArray.addDouble(jsonNumber.doubleValue());
			}
		} else {
			throw new CommandDocumentException(Code.JSON_TO_COMMAND_DOCUMENT_ERROR, "GLUE array may not contain JSON value "+jsonValue);
		}
	}

	private static void populateCommandObjectFromJsonObject(CommandObject commandObject, JsonObject jsonObject) {
		jsonObject.forEach((key, value) -> {
			populateChildCommandObjectFromJsonValue(commandObject, key, value);
		});
	}


	
}
