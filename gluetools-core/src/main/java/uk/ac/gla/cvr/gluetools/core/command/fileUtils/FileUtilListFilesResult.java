package uk.ac.gla.cvr.gluetools.core.command.fileUtils;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class FileUtilListFilesResult extends BaseTableResult<String> {

	public FileUtilListFilesResult(List<String> rowObjects) {
		super("fileUtilListFilesResult", rowObjects, column("fileName", s -> s));
	}

}
