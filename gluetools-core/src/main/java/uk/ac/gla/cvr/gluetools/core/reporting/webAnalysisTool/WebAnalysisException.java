package uk.ac.gla.cvr.gluetools.core.reporting.webAnalysisTool;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class WebAnalysisException extends GlueException {

	public enum Code implements GlueErrorCode {

		UNKNOWN_VARIATION_CATEGORY("vCatName"),
		INVALID_CONFIG("errorTxt"),;

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	protected WebAnalysisException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

	public WebAnalysisException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}
	
}
