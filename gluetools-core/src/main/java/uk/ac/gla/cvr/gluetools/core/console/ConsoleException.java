package uk.ac.gla.cvr.gluetools.core.console;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class ConsoleException extends GlueException {

	public enum Code implements GlueErrorCode {
	
		SYNTAX_ERROR("errorPosition"),
		UNKNOWN_COMMAND("unknownCommand", "commandModePath"), 
		COMMAND_USAGE_ERROR("errorText"), 
		ARGUMENT_FORMAT_ERROR("argName", "errorText", "argValue");

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
