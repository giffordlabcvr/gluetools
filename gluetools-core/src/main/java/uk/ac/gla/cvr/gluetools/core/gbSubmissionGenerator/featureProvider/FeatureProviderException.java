package uk.ac.gla.cvr.gluetools.core.gbSubmissionGenerator.featureProvider;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class FeatureProviderException extends GlueException {

		public enum Code implements GlueErrorCode {

			UNABLE_TO_ESTABLISH_ALIGNMENT_MEMBER("sourceName", "sequenceID", "errorTxt"),
			CONFIG_ERROR("errorTxt"),
			NO_INTERVALS_GENERATED("sequenceID", "featureKey"),
			FEATURE_LOCATION_NOT_FOUND_ON_REFERENCE("featureName", "referenceName"),
			FEATURE_LOCATION_EMPTY_ON_REFERENCE("featureName", "referenceName");

			private String[] argNames;
			private Code(String... argNames) {
				this.argNames = argNames;
			}
			@Override
			public String[] getArgNames() {
				return argNames;
			}
		}
		
		
		public FeatureProviderException(GlueErrorCode code, Object... errorArgs) {
			super(code, errorArgs);
		}

		public FeatureProviderException(Throwable cause, GlueErrorCode code,
				Object... errorArgs) {
			super(cause, code, errorArgs);
		}


}
