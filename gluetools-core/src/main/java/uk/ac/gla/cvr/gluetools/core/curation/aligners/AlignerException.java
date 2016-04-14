package uk.ac.gla.cvr.gluetools.core.curation.aligners;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class AlignerException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		CANNOT_ALIGN_AGAINST_DISCONTIGUOUS_FEATURE_LOCATION("refName", "featureName"),
		FEATURE_NAME_REQUIRED();

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}

	}
	
	public AlignerException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public AlignerException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
}
