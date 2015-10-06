package uk.ac.gla.cvr.gluetools.core.collation.importing.fasta.alignment;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class FastaAlignmentImporterException extends GlueException {
	
	public enum Code implements GlueErrorCode {
		ALIGNMENT_IS_CONSTRAINED("alignmentName", "referenceName"),
		NO_FASTA_ID_REGEX_MATCH("fastaId"),
		INVALID_WHERE_CLAUSE("fastaId", "whereClause"),
		NO_SEQUENCE_FOUND("fastaId", "whereClause"),
		MULTIPLE_SEQUENCES_FOUND("fastaId", "whereClause"),
		SUBSEQUENCE_NOT_FOUND("startColumnNumber", "endColumnNumber", "fastaId", "whereClause"), 
		MULTIPLE_SUBSEQUENCES_FOUND("startColumnNumber", "endColumnNumber", "fastaId", "whereClause"), 
		MISSING_COVERAGE("startColumnNumber", "endColumnNumber", "fastaId", "whereClause");
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public FastaAlignmentImporterException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public FastaAlignmentImporterException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
}
