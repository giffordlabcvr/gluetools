package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.feature;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class FeatureSegmentException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		FEATURE_SEGMENT_OUT_OF_RANGE("refSeqName", "featureName", "refSeqLength", "refStart", "refEnd"),
		FEATURE_SEGMENT_ENDPOINTS_REVERSED("refSeqName", "featureName", "refStart", "refEnd"),
		FEATURE_SEGMENT_OVERLAPS_EXISTING("refSeqName", "featureName", "refStart", "refEnd", "existingRefSeqStart", "existingRefSeqEnd"),
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
	
	public FeatureSegmentException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public FeatureSegmentException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
