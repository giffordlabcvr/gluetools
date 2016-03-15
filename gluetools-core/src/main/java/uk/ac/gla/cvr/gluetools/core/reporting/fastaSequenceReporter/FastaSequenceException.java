package uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class FastaSequenceException extends GlueException {

	public enum Code implements GlueErrorCode {

		MULTIPLE_FASTA_FILE_SEQUENCES("fileName"), 
		NO_FASTA_FILE_SEQUENCES("fileName"),
		NO_TARGET_REFERENCE_DEFINED(),
		TARGET_REFERENCE_NOT_FOUND("fastaId"),
		TARGET_REFERENCE_AMBIGUOUS("fastaId", "targetRefNames");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	protected FastaSequenceException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

	public FastaSequenceException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}
	
}
