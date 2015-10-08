package uk.ac.gla.cvr.gluetools.programs.blast.refdb;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class BlastRefSeqDBException extends GlueException {

	public enum Code implements GlueErrorCode {

		MAKE_BLAST_DB_FAILED("dbsDir", "projectName", "refName", "exitCode", "stdErr");
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public BlastRefSeqDBException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public BlastRefSeqDBException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
