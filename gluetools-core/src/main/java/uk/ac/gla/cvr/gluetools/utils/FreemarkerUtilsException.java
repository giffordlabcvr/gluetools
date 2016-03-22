package uk.ac.gla.cvr.gluetools.utils;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class FreemarkerUtilsException extends GlueException {

	
	public enum Code implements GlueErrorCode {

		INVALID_FREEMARKER_TEMPLATE("templateString", "errorTxt"),
		FREEMARKER_TEMPLATE_FAILED("errorTxt");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public FreemarkerUtilsException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public FreemarkerUtilsException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
	
}
