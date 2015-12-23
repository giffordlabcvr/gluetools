package uk.ac.gla.cvr.gluetools.core.digs;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class DigsProberException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		BLAST_OUTPUT_FORMAT_ERROR("errorText");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}

	}
	
	public DigsProberException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public DigsProberException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
	
}
