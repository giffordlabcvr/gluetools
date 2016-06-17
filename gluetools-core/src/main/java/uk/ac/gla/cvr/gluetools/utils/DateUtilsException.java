package uk.ac.gla.cvr.gluetools.utils;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class DateUtilsException extends GlueException {

	
	public enum Code implements GlueErrorCode {

		DATE_PARSE_ERROR("errorTxt");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public DateUtilsException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public DateUtilsException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
	
}
