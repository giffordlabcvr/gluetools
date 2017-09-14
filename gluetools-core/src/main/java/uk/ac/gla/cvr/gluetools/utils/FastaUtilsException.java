package uk.ac.gla.cvr.gluetools.utils;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class FastaUtilsException extends GlueException {

	
	public enum Code implements GlueErrorCode {

		FASTA_DOCUMENT_PARSE_ERROR("errorTxt");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public FastaUtilsException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public FastaUtilsException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
	
}
