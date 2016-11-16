package uk.ac.gla.cvr.gluetools.utils;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class ProcessUtilsException extends GlueException {

	public enum Code implements GlueErrorCode {

		UNABLE_TO_START_PROCESS("errorTxt"),
		UNABLE_TO_START_PROCESS_FOR_COMMAND("command", "errorTxt"),
		PROCESS_IO_STDIN_ERROR("command", "errorTxt"),
		PROCESS_IO_STDOUT_ERROR("command", "errorTxt"),
		PROCESS_IO_STDERR_ERROR("command", "errorTxt"),
		PROCESS_EXIT_CODE_ERROR("command", "exitCode");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public ProcessUtilsException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public ProcessUtilsException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
