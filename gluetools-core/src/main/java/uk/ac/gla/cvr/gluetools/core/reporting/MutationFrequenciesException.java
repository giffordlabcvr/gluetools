package uk.ac.gla.cvr.gluetools.core.reporting;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class MutationFrequenciesException extends GlueException {


	public enum Code implements GlueErrorCode {

		UNABLE_TO_DETECT_ALIGNMENT_NAME("header"), 
		FEATURE_CODON_NUMBERING_MISMATCH("orfFeature", "descendentFeature", "orfFeatureCodon1Start", "descendentFeatureCodon1Start"), 
		ORF_MUST_HAVE_SINGLE_SEGMENT("orfFeature", "actualNumberOfFeatures"), 
		ORF_INCOMPLETE_TRANSCRIPTION("orfFeature", "actualTranscribedLength", "expectedTranscribedLength"), 
		ORF_LENGTH_NOT_MULTIPLE_OF_3("orfFeature", "ntLength"),
		ALIGNMENT_IS_UNCONSTRAINED("alignmentName"),
		REF_SEQUENCE_DOES_NOT_CONSTRAIN_ANCESTOR("alignmentName", "referenceName"), 
		FEATURE_LOCATION_NOT_DEFINED("referenceName", "featureName"), 
		FEATURE_IS_INFORMATIONAL("featureName"),
		FEATURE_IS_NOT_IN_ANY_ORF("featureName");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}


	protected MutationFrequenciesException(Throwable cause, Code code,
			Object[] errorArgs) {
		super(cause, code, errorArgs);
	}

	public MutationFrequenciesException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

}
