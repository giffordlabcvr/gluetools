package uk.ac.gla.cvr.gluetools.core.datamodel.feature;

import uk.ac.gla.cvr.gluetools.core.GlueException;

@SuppressWarnings("serial")
public class FeatureException extends GlueException {

	public enum Code implements GlueErrorCode {
		PARENT_RELATIONSHIP_LOOP("loopNames"), 
		FEATURE_WITH_OWN_CODON_NUMBERING_NOT_IN_ORF("featureName");
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}
	
	public FeatureException(GlueErrorCode code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public FeatureException(Throwable cause, GlueErrorCode code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
