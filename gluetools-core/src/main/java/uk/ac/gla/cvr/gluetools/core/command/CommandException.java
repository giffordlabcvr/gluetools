package uk.ac.gla.cvr.gluetools.core.command;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class CommandException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		UNKNOWN_COMMAND("unknownCommandText", "commandModePath"), 
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
	
	public CommandException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public CommandException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

	
}
