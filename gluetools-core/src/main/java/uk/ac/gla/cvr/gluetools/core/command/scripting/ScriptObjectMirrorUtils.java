package uk.ac.gla.cvr.gluetools.core.command.scripting;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jdk.nashorn.api.scripting.ScriptObjectMirror;
import uk.ac.gla.cvr.gluetools.core.command.scripting.ScriptObjectMirrorUtilsException.Code;
import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.utils.DateUtils;

public class ScriptObjectMirrorUtils {

	
	public static CommandDocument scriptObjectMirrorToCommandDocument(ScriptObjectMirror scrObjMirror) {
		List<String> jsonKeySet = new ArrayList<String>(scrObjMirror.keySet());
		if(jsonKeySet.size() != 1) {
			throw new ScriptObjectMirrorUtilsException(Code.JS_OBJECT_TO_COMMAND_DOCUMENT_ERROR, "Root JavaScript object must have a single key");
		}
		String rootName = jsonKeySet.get(0);
		CommandDocument documentBuilder = new CommandDocument(rootName);
		Object rootObject = scrObjMirror.get(rootName);
		if(!(rootObject instanceof ScriptObjectMirror)) {
			throw new ScriptObjectMirrorUtilsException(Code.JS_OBJECT_TO_COMMAND_DOCUMENT_ERROR, "Root JavaScript object must have an object as its single value");
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
			Number num = (Number) value;
			// javascript does not have integers, only floats
			// here we force integer if the number is mathematically an integer.
			double doubleVal = Math.round(num.doubleValue());
			if(doubleVal == num.doubleValue()) {
				parentCommandObject.setInt(key, num.intValue());
			} else {
				parentCommandObject.setDouble(key, num.doubleValue());
			}
		} else {
			throw new ScriptObjectMirrorUtilsException(Code.JS_OBJECT_TO_COMMAND_DOCUMENT_ERROR, "GLUE object may not contain JavaScript value "+value);
		}
	}

	
	private static void addArrayItemFromValue(CommandArray commandArray, Object value) {
		if(value instanceof ScriptObjectMirror) {
			CommandObject childCommandObject = commandArray.addObject();
			populateCommandObjectFromScriptObjectMirror(childCommandObject, (ScriptObjectMirror) value);
		} else if(value instanceof Collection) {
			throw new ScriptObjectMirrorUtilsException(Code.JS_OBJECT_TO_COMMAND_DOCUMENT_ERROR, "GLUE array may not contain JavaScript array as item");
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
			Number num = (Number) value;
			// javascript does not have integers, only floats
			// here we force integer if the number is mathematically an integer.
			double doubleVal = Math.round(num.doubleValue());
			if(doubleVal == num.doubleValue()) {
				commandArray.addInt(num.intValue());
			} else {
				commandArray.addDouble(doubleVal);
			}
		} else {
			throw new ScriptObjectMirrorUtilsException(Code.JS_OBJECT_TO_COMMAND_DOCUMENT_ERROR, "GLUE array may not contain JavaScript value "+value);
		}
	}

		
}
