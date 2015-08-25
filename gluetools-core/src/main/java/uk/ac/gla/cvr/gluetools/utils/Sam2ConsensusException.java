package uk.ac.gla.cvr.gluetools.utils;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class Sam2ConsensusException extends GlueException {
	
	public enum Code implements GlueErrorCode {

		FORMAT_ERROR("errorTxt");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public Sam2ConsensusException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public Sam2ConsensusException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
