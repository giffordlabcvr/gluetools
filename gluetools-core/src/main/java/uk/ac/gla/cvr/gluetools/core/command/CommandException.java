package uk.ac.gla.cvr.gluetools.core.command;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class CommandException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		UNKNOWN_COMMAND("unknownCommandText", "commandModePath"), 
		NOT_A_MODE_COMMAND("commandText", "commandModePath"), 
		COMMAND_USAGE_ERROR("errorText"), 
		COMMAND_FAILED_ERROR("errorText"), 
		COMMAND_DOES_NOT_CONSUME_BINARY("commandWords"), 
		NOT_EXECUTABLE_IN_CONTEXT("commandWords", "contextDescription"), 
		ARGUMENT_FORMAT_ERROR("argName", "errorText", "argValue"), 
		COMMAND_BINARY_INPUT_IO_ERROR("commandWords", "errorText"), 
		UNKNOWN_MODE_PATH("commandModePath");

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
