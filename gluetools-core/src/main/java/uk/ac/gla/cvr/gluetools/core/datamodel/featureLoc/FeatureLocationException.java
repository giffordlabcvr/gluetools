package uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc;

import uk.ac.gla.cvr.gluetools.core.GlueException;

@SuppressWarnings("serial")
public class FeatureLocationException extends GlueException {

	public enum Code implements GlueErrorCode {
		NEXT_ANCESTOR_FEATURE_LOCATION_UNDEFINED("refSeqName", "featureName", "nextAncestorFeatureName"),
		FEATURE_LOCATION_NOT_CONTAINED_WITHIN_NEXT_ANCESTOR("refSeqName", "featureName", "nextAncestorFeatureName"), 
		FEATURE_LOCATION_HAS_NO_SEGMENTS("refSeqName", "featureName"), 
		FEATURE_LOCATION_SEGMENT_NOT_CODON_ALIGNED("refSeqName", "featureName", "segRefStart", "segRefEnd", "codon1Start"),
		FEATURE_LOCATION_ORF_TRANSCRIPTION_INCOMPLETE("refSeqName", "featureName", "segRefStart", "segRefEnd");
		
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}
	
	public FeatureLocationException(GlueErrorCode code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public FeatureLocationException(Throwable cause, GlueErrorCode code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}