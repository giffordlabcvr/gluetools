package uk.ac.gla.cvr.gluetools.utils;

import java.util.Date;
import java.util.LinkedList;

import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.document.CommandArrayItem;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocumentVisitor;
import uk.ac.gla.cvr.gluetools.core.document.CommandFieldValue;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.core.document.SimpleCommandValue;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentException.Code;
import uk.ac.gla.cvr.gluetools.utils.GlueTypeUtils.GlueType;

public class JsonObjectCmdDocVisitor implements CommandDocumentVisitor {

	private JsonObjectBuilder rootJsonObjectBuilder;
	private JsonObject rootJsonObject;
	private LinkedList<Object> jsonBuilderStack = new LinkedList<Object>();
	
	public JsonObjectCmdDocVisitor() {
	}
	
	@Override
	public void preVisitCommandDocument(CommandDocument commandDocument) {
		this.rootJsonObjectBuilder = JsonUtils.jsonObjectBuilder();
		jsonBuilderStack.push(rootJsonObjectBuilder);
	}

	@Override
	public void postVisitCommandDocument(CommandDocument commandDocument) {
		this.rootJsonObject = this.rootJsonObjectBuilder.build();
	}
	
	@Override
	public void preVisitCommandObject(String fieldName, CommandObject commandObject) {
		JsonObjectBuilder nextObjBuilder = JsonUtils.jsonObjectBuilder();
		jsonBuilderStack.push(nextObjBuilder);
	}

	@Override
	public void postVisitCommandObject(String fieldName, CommandObject commandObject) {
		JsonObjectBuilder poppedJsonObjectBuilder = (JsonObjectBuilder) jsonBuilderStack.pop();
		JsonObject poppedJsonObject = poppedJsonObjectBuilder.build();
		JsonObjectBuilder currentJsonObjectBuilder = currentJsonObjectBuilder();
		if(currentJsonObjectBuilder != null) {
			currentJsonObjectBuilder.add(fieldName, poppedJsonObject);
		} else {
			currentJsonArrayBuilder().add(poppedJsonObject);
		}
	}

	@Override
	public void preVisitCommandArray(String fieldName, CommandArray commandArray) {
		JsonArrayBuilder nextArrayBuilder = JsonUtils.jsonArrayBuilder();
		jsonBuilderStack.push(nextArrayBuilder);
	}


	@Override
	public void postVisitCommandArray(String fieldName, CommandArray commandArray) {
		JsonArrayBuilder poppedJsonArrayBuilder = (JsonArrayBuilder) jsonBuilderStack.pop();
		JsonArray poppedJsonArray = poppedJsonArrayBuilder.build();
		JsonObjectBuilder currentJsonObjectBuilder = currentJsonObjectBuilder();
		if(currentJsonObjectBuilder != null) {
			currentJsonObjectBuilder.add(fieldName, poppedJsonArray);
		} else {
			currentJsonArrayBuilder().add(poppedJsonArray);
		}
	}

	private JsonObjectBuilder currentJsonObjectBuilder() {
		Object currentBuilder = jsonBuilderStack.peek();
		if(currentBuilder instanceof JsonObjectBuilder) {
			return (JsonObjectBuilder) currentBuilder;
		}
		return null;
	}

	private JsonArrayBuilder currentJsonArrayBuilder() {
		Object currentBuilder = jsonBuilderStack.peek();
		if(currentBuilder instanceof JsonArrayBuilder) {
			return (JsonArrayBuilder) currentBuilder;
		}
		return null;
	}

	@Override
	public void preVisitCommandFieldValue(String fieldName, CommandFieldValue commandFieldValue) {
		JsonObjectBuilder currentJsonObjectBuilder = currentJsonObjectBuilder();
		if(commandFieldValue instanceof SimpleCommandValue) {
			addSimpleValueToObjectBuilder(currentJsonObjectBuilder, fieldName, (SimpleCommandValue) commandFieldValue);
		} 
	}
	
	@Override
	public void preVisitCommandArrayItem(String arrayFieldName, CommandArrayItem commandArrayItem) {
		JsonArrayBuilder currentJsonArrayBuilder = currentJsonArrayBuilder();
		if(commandArrayItem instanceof SimpleCommandValue) {
			addSimpleValueToArrayBuilder(currentJsonArrayBuilder, (SimpleCommandValue) commandArrayItem);
		} 
	}

	public JsonObject getJsonObject() {
		return rootJsonObject;
	}
	
	private void addSimpleValueToArrayBuilder(JsonArrayBuilder arrayBuilder, SimpleCommandValue simpleCommandValue) {
		GlueType glueType = simpleCommandValue.getGlueType();
		switch(glueType) {
		case Double:
			arrayBuilder.add((Double) simpleCommandValue.getValue());
			break;
		case Integer:
			arrayBuilder.add((Integer) simpleCommandValue.getValue());
			break;
		case Boolean:
			arrayBuilder.add((Boolean) simpleCommandValue.getValue());
			break;
		case Date:
			arrayBuilder.add(DateUtils.render((Date) simpleCommandValue.getValue()));
			break;
		case String:
			arrayBuilder.add((String) simpleCommandValue.getValue());
			break;
		case Null:
			arrayBuilder.addNull();
			break;
		default:
			throw new CommandDocumentException(Code.COMMAND_DOCUMENT_TO_JSON_ERROR, "Not a simple type: "+glueType.name());
		}
	}

	private void addSimpleValueToObjectBuilder(JsonObjectBuilder objBuilder, String fieldName, SimpleCommandValue simpleCommandValue) {
		GlueType glueType = simpleCommandValue.getGlueType();
		switch(glueType) {
		case Double:
			objBuilder.add(fieldName, (Double) simpleCommandValue.getValue());
			break;
		case Integer:
			objBuilder.add(fieldName, (Integer) simpleCommandValue.getValue());
			break;
		case Boolean:
			objBuilder.add(fieldName, (Boolean) simpleCommandValue.getValue());
			break;
		case Date:
			objBuilder.add(fieldName, DateUtils.render((Date) simpleCommandValue.getValue()));
			break;
		case String:
			objBuilder.add(fieldName, (String) simpleCommandValue.getValue());
			break;
		case Null:
			objBuilder.addNull(fieldName);
			break;
		default:
			throw new CommandDocumentException(Code.COMMAND_DOCUMENT_TO_JSON_ERROR, "Not a simple type: "+glueType.name());
		}
	}

	
}
