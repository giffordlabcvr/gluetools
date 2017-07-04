package uk.ac.gla.cvr.gluetools.core.command.scripting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentException;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentException.Code;
import uk.ac.gla.cvr.gluetools.utils.DateUtils;

public class ScriptObjectMirrorUtils {

	
	public static CommandDocument scriptObjectMirrorToCommandDocument(ScriptObjectMirror scrObjMirror) {
		List<String> jsonKeySet = new ArrayList<String>(scrObjMirror.keySet());
		if(jsonKeySet.size() != 1) {
			throw new CommandDocumentException(Code.JSON_TO_COMMAND_DOCUMENT_ERROR, "Root JSON object must have a single key");
		}
		String rootName = jsonKeySet.get(0);
		CommandDocument documentBuilder = new CommandDocument(rootName);
		Object rootObject = scrObjMirror.get(rootName);
		if(!(rootObject instanceof ScriptObjectMirror)) {
			throw new CommandDocumentException(Code.JSON_TO_COMMAND_DOCUMENT_ERROR, "Root JSON object must have an object as its single value");
		}
		populateCommandObjectFromScriptObjectMirror(documentBuilder, (ScriptObjectMirror) rootObject);
		return documentBuilder;
	}


	private static void populateCommandObjectFromScriptObjectMirror(CommandObject commandObject, ScriptObjectMirror scrObjMirror) {
		scrObjMirror.forEach((key, value) -> {
			populateChildCommandObjectFromValue(commandObject, key, value);
		});
	}

	private static void populateChildCommandObjectFromValue(CommandObject parentCommandObject, String key, Object value) {
		if(value instanceof ScriptObjectMirror) {
			ScriptObjectMirror scriptObjectMirror = (ScriptObjectMirror) value;
			if(scriptObjectMirror.isArray()) {
				CommandArray arrayBuilder = parentCommandObject.setArray(key);
				for(int i = 0; i < scriptObjectMirror.size(); i++) {
					addArrayItemFromValue(arrayBuilder, scriptObjectMirror.getSlot(i));
				}
			} else {
				CommandObject childCommandObject = parentCommandObject.setObject(key);
				populateCommandObjectFromScriptObjectMirror(childCommandObject, (ScriptObjectMirror) value);
			}
		} else if(value == null){
			parentCommandObject.setNull(key);
		} else if(value instanceof String) {
			String string = (String) value;
			if(DateUtils.isDateString(string)) {
				parentCommandObject.setDate(key, DateUtils.parse(string));
			} else {
				parentCommandObject.setString(key, string);
			}
		} else if(value instanceof Boolean) {
			parentCommandObject.setBoolean(key, (Boolean) value);
		} else if(value instanceof Number) {
			Number number = (Number) value;
			if(number instanceof Integer) {
				parentCommandObject.setInt(key, number.intValue());
			} else {
				parentCommandObject.setDouble(key, number.doubleValue());
			}
		} else {
			throw new CommandDocumentException(Code.JSON_TO_COMMAND_DOCUMENT_ERROR, "GLUE object may not contain JSON value "+value);
		}
	}

	
	private static void addArrayItemFromValue(CommandArray commandArray, Object value) {
		if(value instanceof ScriptObjectMirror) {
			CommandObject childCommandObject = commandArray.addObject();
			populateCommandObjectFromScriptObjectMirror(childCommandObject, (ScriptObjectMirror) value);
		} else if(value instanceof Collection) {
			throw new CommandDocumentException(Code.JSON_TO_COMMAND_DOCUMENT_ERROR, "GLUE array may not contain JSON array as item");
		} else if(value == null){
			commandArray.addNull();
		} else if(value instanceof String) {
			String string = (String) value;
			if(DateUtils.isDateString(string)) {
				commandArray.addDate(DateUtils.parse(string));
			} else {
				commandArray.addString(string);
			}
		} else if(value instanceof Boolean) {
			commandArray.addBoolean((Boolean) value);
		} else if(value instanceof Number) {
			Number number = (Number) value;
			if(number instanceof Integer) {
				commandArray.addInt(number.intValue());
			} else {
				commandArray.addDouble(number.doubleValue());
			}
		} else {
			throw new CommandDocumentException(Code.JSON_TO_COMMAND_DOCUMENT_ERROR, "GLUE array may not contain JSON value "+value);
		}
	}

		
}
