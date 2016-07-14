package uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class MaxLikelihoodGenotyperException extends GlueException {

public enum Code implements GlueErrorCode {
		
		CONFIG_ERROR("errorTxt");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public MaxLikelihoodGenotyperException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public MaxLikelihoodGenotyperException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
}
