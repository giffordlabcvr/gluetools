package uk.ac.gla.cvr.gluetools.core.command.scripting;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class ScriptObjectMirrorUtilsException extends GlueException {

	
	public enum Code implements GlueErrorCode {

		JS_OBJECT_TO_COMMAND_DOCUMENT_ERROR("errorTxt");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public ScriptObjectMirrorUtilsException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public ScriptObjectMirrorUtilsException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
	
}
