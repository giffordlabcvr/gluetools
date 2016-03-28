package uk.ac.gla.cvr.gluetools.core.commonAaPolymorphisms;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class CommonAaPolymorphismException extends GlueException {

	public enum Code implements GlueErrorCode {

		GAP_AT_START;

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	protected CommonAaPolymorphismException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

	public CommonAaPolymorphismException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}
	
	
}
