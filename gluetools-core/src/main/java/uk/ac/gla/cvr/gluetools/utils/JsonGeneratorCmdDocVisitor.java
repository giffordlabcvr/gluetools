package uk.ac.gla.cvr.gluetools.utils;

import java.util.Date;

import javax.json.stream.JsonGenerator;

import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.document.CommandArrayItem;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocumentVisitor;
import uk.ac.gla.cvr.gluetools.core.document.CommandFieldValue;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.core.document.SimpleCommandValue;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentException.Code;
import uk.ac.gla.cvr.gluetools.utils.GlueTypeUtils.GlueType;

public class JsonGeneratorCmdDocVisitor implements CommandDocumentVisitor {

	private JsonGenerator jsonGenerator;
	
	public JsonGeneratorCmdDocVisitor(JsonGenerator jsonGenerator) {
		this.jsonGenerator = jsonGenerator;
	}
	
	@Override
	public void preVisitCommandDocument(CommandDocument commandDocument) {
		jsonGenerator.writeStartObject();
		jsonGenerator.writeStartObject(commandDocument.getRootName());
	}

	@Override
	public void postVisitCommandDocument(CommandDocument commandDocument) {
		jsonGenerator.writeEnd();
		jsonGenerator.writeEnd();
	}
	
	@Override
	public void preVisitCommandArray(String arrayFieldName, CommandArray commandArray) {
		jsonGenerator.writeStartArray(arrayFieldName);
	}


	@Override
	public void postVisitCommandArray(String fieldName, CommandArray commandArray) {
		jsonGenerator.writeEnd();
	}

	@Override
	public void visitCommandFieldValue(String fieldName, CommandFieldValue commandFieldValue) {
		if(commandFieldValue instanceof SimpleCommandValue) {
			writeSimpleValueToObject(fieldName, (SimpleCommandValue) commandFieldValue);
		} else if(commandFieldValue instanceof CommandObject) {
			CommandObject commandObject = (CommandObject) commandFieldValue;
			jsonGenerator.writeStartObject(fieldName);
			commandObject.accept(fieldName, this);
			jsonGenerator.writeEnd();
		} else if(commandFieldValue instanceof CommandArray) {
			CommandArray arrayBuilder = (CommandArray) commandFieldValue;
			arrayBuilder.accept(fieldName, this);
		} 
	}
	
	@Override
	public void visitCommandArrayItem(String arrayFieldName, CommandArrayItem commandArrayItem) {
		if(commandArrayItem instanceof SimpleCommandValue) {
			writeSimpleValueToArray((SimpleCommandValue) commandArrayItem);
		} else if(commandArrayItem instanceof CommandObject) {
			CommandObject commandObject = (CommandObject) commandArrayItem;
			jsonGenerator.writeStartObject();
			commandObject.accept(arrayFieldName, this);
			jsonGenerator.writeEnd();
		} 
	}

	private void writeSimpleValueToObject(String fieldName, SimpleCommandValue simpleCommandValue) {
		GlueType glueType = simpleCommandValue.getGlueType();
		switch(glueType) {
		case Double:
			jsonGenerator.write(fieldName, (Double) simpleCommandValue.getValue());
			break;
		case Integer:
			jsonGenerator.write(fieldName, (Integer) simpleCommandValue.getValue());
			break;
		case Boolean:
			jsonGenerator.write(fieldName, (Boolean) simpleCommandValue.getValue());
			break;
		case Date:
			jsonGenerator.write(fieldName, DateUtils.render((Date) simpleCommandValue.getValue()));
			break;
		case String:
			jsonGenerator.write(fieldName, (String) simpleCommandValue.getValue());
			break;
		case Null:
			jsonGenerator.writeNull(fieldName);
			break;
		default:
			throw new CommandDocumentException(Code.COMMAND_DOCUMENT_TO_JSON_ERROR, "Not a simple type: "+glueType.name());
		}
	}

	private void writeSimpleValueToArray(SimpleCommandValue simpleCommandValue) {
		GlueType glueType = simpleCommandValue.getGlueType();
		switch(glueType) {
		case Double:
			jsonGenerator.write((Double) simpleCommandValue.getValue());
			break;
		case Integer:
			jsonGenerator.write((Integer) simpleCommandValue.getValue());
			break;
		case Boolean:
			jsonGenerator.write((Boolean) simpleCommandValue.getValue());
			break;
		case Date:
			jsonGenerator.write(DateUtils.render((Date) simpleCommandValue.getValue()));
			break;
		case String:
			jsonGenerator.write((String) simpleCommandValue.getValue());
			break;
		case Null:
			jsonGenerator.writeNull();
			break;
		default:
			throw new CommandDocumentException(Code.COMMAND_DOCUMENT_TO_JSON_ERROR, "Not a simple type: "+glueType.name());
		}
	}
}