package uk.ac.gla.cvr.gluetools.core.reporting.objectRenderer;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class ObjectRendererException extends GlueException {

	public enum Code implements GlueErrorCode {

		INVALID_XML_PRODUCED("errorText");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public ObjectRendererException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

	public ObjectRendererException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	
}
