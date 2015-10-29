package uk.ac.gla.cvr.gluetools.core.command.console.config;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class ConsoleOptionException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		NO_SUCH_OPTION("optionName", "validOptions"),
		INVALID_OPTION_VALUE("optionName", "badValue", "errorTxt");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}

	}

	public ConsoleOptionException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public ConsoleOptionException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
	
}
