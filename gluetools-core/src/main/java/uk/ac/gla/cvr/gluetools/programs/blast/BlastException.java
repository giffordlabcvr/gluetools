package uk.ac.gla.cvr.gluetools.programs.blast;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class BlastException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		BLAST_OUTPUT_FORMAT_ERROR("errorText"),
		BLAST_UNHANDLED_CASE("refSeqId", "querySeqId", "caseDescription"),
		UNKNOWN_BLAST_TYPE("blastType");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}

	}
	
	public BlastException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public BlastException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
	
}
