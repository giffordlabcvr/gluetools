package uk.ac.gla.cvr.gluetools.core.curation.aligners;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class BlastAlignerException extends GlueException {

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
	
	public BlastAlignerException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public BlastAlignerException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
	
}
