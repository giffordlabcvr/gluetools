package uk.ac.gla.cvr.gluetools.core.collation.freemarker;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class FreemarkerTextToGlueException extends GlueException {

	public enum Code implements GlueErrorCode {
		TEMPLATE_PROCESSING_FAILED("errorTxt");
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public FreemarkerTextToGlueException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public FreemarkerTextToGlueException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
	
}
