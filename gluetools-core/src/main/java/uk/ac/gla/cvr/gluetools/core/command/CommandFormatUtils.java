package uk.ac.gla.cvr.gluetools.core.command;

import javax.json.JsonObject;

import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentJsonUtils;
import uk.ac.gla.cvr.gluetools.utils.JsonUtils;

public class CommandFormatUtils {

	public static CommandDocument commandDocumentFromJsonString(String commandString) {
		JsonObject jsonObject;
		try {
			jsonObject = JsonUtils.stringToJsonObject(commandString);
		} catch(Exception e) {
			throw new CommandFormatException(e, CommandFormatException.Code.GLUE_COMMAND_JSON_MALFORMED, e.getMessage());
		}
		return CommandDocumentJsonUtils.jsonObjectToCommandDocument(jsonObject);
	}

	
}
