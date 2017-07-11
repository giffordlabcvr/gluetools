package uk.ac.gla.cvr.gluetools.core.command.fileUtils;

import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;

public class FileUtilDeleteFileResult extends MapResult {

	public FileUtilDeleteFileResult(int numFilesDeleted) {
		super("fileUtilListFilesResult",  mapBuilder().put("numFilesDeleted", numFilesDeleted));
	}

}
