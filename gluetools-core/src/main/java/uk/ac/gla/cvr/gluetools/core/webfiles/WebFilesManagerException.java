package uk.ac.gla.cvr.gluetools.core.webfiles;

import uk.ac.gla.cvr.gluetools.core.GlueException;
import uk.ac.gla.cvr.gluetools.core.GlueException.GlueErrorCode;


public class WebFilesManagerException extends GlueException {

	public enum Code implements GlueErrorCode {

		INVALID_ROOT_PATH("rootPath"),
		WEB_FILES_MANAGER_NOT_ENABLED,
		SUBDIR_CREATION_FAILED("subDirPath");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public WebFilesManagerException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public WebFilesManagerException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
