package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class MaxLikelihoodPlacerException extends GlueException {

public enum Code implements GlueErrorCode {
		
		CONFIG_ERROR("errorTxt"),
		INPUT_ERROR("errorTxt"),
		JPLACE_BRANCH_LABEL_ERROR("errorTxt"),
		JPLACE_STRUCTURE_ERROR("errorTxt"),
		POJO_RESULT_FORMAT_ERROR("errorTxt");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public MaxLikelihoodPlacerException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public MaxLikelihoodPlacerException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
}
