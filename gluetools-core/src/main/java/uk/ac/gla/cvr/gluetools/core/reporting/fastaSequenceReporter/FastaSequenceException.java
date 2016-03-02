package uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class FastaSequenceException extends GlueException {

	public enum Code implements GlueErrorCode {

		MULTIPLE_FASTA_FILE_SEQUENCES("fileName"), 
		NO_FASTA_FILE_SEQUENCES("fileName"),
		NO_GLUE_REFERENCE_DEFINED,
		TIP_ALIGNMENT_MEMBER_NOT_FOUND("fastaID", "whereClause"),
		TIP_ALIGNMENT_MEMBER_EXTRACTOR_FAILED("fastaID"),
		AMBIGUOUS_TIP_ALIGNMENT_MEMBER_DEFINED();

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
