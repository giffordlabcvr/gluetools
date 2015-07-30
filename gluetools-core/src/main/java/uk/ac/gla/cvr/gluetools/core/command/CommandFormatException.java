package uk.ac.gla.cvr.gluetools.core.command;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class CommandFormatException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		GLUE_COMMAND_JSON_MALFORMED("errorText"),
		GLUE_COMMAND_JSON_SINGLE_KEY(),
		GLUE_COMMAND_JSON_VALUE_NOT_OBJECT();

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}

	}
	
	public CommandFormatException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public CommandFormatException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
	
}
