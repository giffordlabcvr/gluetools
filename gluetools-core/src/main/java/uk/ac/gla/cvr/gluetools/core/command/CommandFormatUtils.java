package uk.ac.gla.cvr.gluetools.core.command;

import java.util.Map;

import javax.json.JsonObject;
import javax.json.JsonValue;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;

public class CommandFormatUtils {

	public static Element cmdDocElemFromJsonString(String commandString) {
		JsonObject jsonObject;
		try {
			jsonObject = JsonUtils.stringToJsonObject(commandString);
		} catch(Exception e) {
			throw new CommandFormatException(e, CommandFormatException.Code.GLUE_COMMAND_JSON_MALFORMED, e.getMessage());
		}
		if(jsonObject.entrySet().size() != 1) {
			throw new CommandFormatException(CommandFormatException.Code.GLUE_COMMAND_JSON_SINGLE_KEY);
		}
		Map.Entry<String, JsonValue> singleEntry = jsonObject.entrySet().iterator().next();
		String key = singleEntry.getKey();
		JsonValue jsonValue = singleEntry.getValue();
		if(!(jsonValue instanceof JsonObject)) {
			throw new CommandFormatException(CommandFormatException.Code.GLUE_COMMAND_JSON_VALUE_NOT_OBJECT);
		}
		Element cmdDocElem = GlueXmlUtils.documentWithElement(key);
		JsonUtils.jsonObjectToElement(cmdDocElem, (JsonObject) jsonValue);
		return cmdDocElem;
	}

	
}
