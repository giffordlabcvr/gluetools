package uk.ac.gla.cvr.gluetools.core.datamodel.featureMetatag;

import uk.ac.gla.cvr.gluetools.core.GlueException;

@SuppressWarnings("serial")
public class FeatureMetatagException extends GlueException {

	public enum Code implements GlueErrorCode {
		UNKNOWN_FEATURE_METATAG("unknownMetatag");
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}
	
	public FeatureMetatagException(GlueErrorCode code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public FeatureMetatagException(Throwable cause, GlueErrorCode code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
