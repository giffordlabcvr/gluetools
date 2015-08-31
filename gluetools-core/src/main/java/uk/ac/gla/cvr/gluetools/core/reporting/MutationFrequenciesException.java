package uk.ac.gla.cvr.gluetools.core.reporting;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class MutationFrequenciesException extends GlueException {


	public enum Code implements GlueErrorCode {

		UNABLE_TO_DETECT_ALIGNMENT_NAME("header");

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
