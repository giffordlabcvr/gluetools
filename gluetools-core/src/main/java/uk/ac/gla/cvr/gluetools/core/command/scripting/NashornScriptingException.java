package uk.ac.gla.cvr.gluetools.core.command.scripting;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class NashornScriptingException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		SCRIPT_EXCEPTION("fileName", "lineNumber", "columnNumber", "errorTxt", "jsStackTrace"),
		UNKNOWN_COMMAND("jsonString", "modePath"),
		COMMAND_INPUT_ERROR("errorTxt"); 

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public NashornScriptingException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public NashornScriptingException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}


	
}
