package uk.ac.gla.cvr.gluetools.utils;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class CayenneUtilsException extends GlueException {

	public enum Code implements GlueErrorCode {

		INVALID_CAYENNE_EXPRESSION("expressionString", "errorTxt");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public CayenneUtilsException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public CayenneUtilsException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
