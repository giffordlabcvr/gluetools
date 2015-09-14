package uk.ac.gla.cvr.gluetools.core.datamodel.variation;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class VariationException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		VARIATION_LOCATION_OUT_OF_RANGE("refSeqName", "featureName", 
				"variationName", "refSeqLength", "refStart", "refEnd"),
		VARIATION_ENDPOINTS_REVERSED("refSeqName", "featureName", 
				"variationName", "refStart", "refEnd")
		;

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}
	
	public VariationException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public VariationException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

	
}
