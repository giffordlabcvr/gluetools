package uk.ac.gla.cvr.gluetools.programs.blast.dbManager;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class BlastDbManagerException extends GlueException {

	public enum Code implements GlueErrorCode {

		MAKE_BLAST_DB_FAILED("dbDir", "dbTitle", "exitCode", "stdErr");
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public BlastDbManagerException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public BlastDbManagerException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
