package uk.ac.gla.cvr.gluetools.core.command;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;

public class CommandWebFileResult extends CommandResult {

	public static final String WEB_FILE_NAME_PROPERTY = "webFileName";
	public static final String WEB_SUB_DIR_UUID_PROPERTY = "webSubDirUuid";
	public static final String WEB_FILE_SIZE_STRING = "webFileSizeString";
	
	public CommandWebFileResult(String rootObjectName, String webSubDirUuid, String webFileName, String webFileSizeString) {
		super(rootObjectName);
		getCommandDocument().set(WEB_FILE_NAME_PROPERTY, webFileName);
		getCommandDocument().set(WEB_SUB_DIR_UUID_PROPERTY, webSubDirUuid);
		getCommandDocument().set(WEB_FILE_SIZE_STRING, webFileSizeString);
	}

}
