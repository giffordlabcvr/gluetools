package uk.ac.gla.cvr.gluetools.core.console;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class ConsoleException extends GlueException {

	public enum Code implements GlueErrorCode {
	
		SYNTAX_ERROR("errorPosition"),
		INVALID_PATH("invalidPath", "errorText"), 
		FILE_NOT_FOUND("path"), 
		NOT_A_FILE("path"), 
		FILE_NOT_READABLE("path"), 
		FILE_NOT_WRITEABLE("path"), 
		READ_ERROR("path", "errorTxt"),
		WRITE_ERROR("path", "errorTxt"),
		FILE_CREATION_ERROR("path", "errorTxt"),
		COMMAND_NOT_WRAPPABLE("commandWords", "commandModePath");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}

	}
	
	public ConsoleException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public ConsoleException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
