package uk.ac.gla.cvr.gluetools.utils;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class GlueXmlUtilsException extends GlueException {

	public enum Code implements GlueErrorCode {

		XML_PARSE_EXCEPTION("errorTxt");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public GlueXmlUtilsException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public GlueXmlUtilsException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
	
}
