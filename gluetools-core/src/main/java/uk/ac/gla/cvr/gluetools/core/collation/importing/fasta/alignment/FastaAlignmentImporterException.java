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
		AMBIGUOUS_SEGMENT("startColumnNumber", "endColumnNumber", "fastaId", "whereClause", "fromPosition"), 
		MISSING_COVERAGE("startColumnNumber", "endColumnNumber", "fastaId", "whereClause"), 
		SEGMENT_OVERLAPS_EXISTING("startColumnNumber", "endColumnNumber", "fastaId", "whereClause"), 
		NAVIGATION_ALIGNMENT_REQUIRED(),
		NAVIGATION_ALIGNMENT_IS_UNCONSTRAINED("navAlignmentName"),
		NAVIGATION_ALIGNMENT_MEMBER_NOT_FOUND("navAlignmentName", "memberSourceName", "memberSequenceID"),
		NAVIGATION_REF_SEQ_FEATURE_MISSING("navRefSeqName", "navAlignmentName", "featureName"),
		NAVIGATION_REF_SEQ_FEATURE_HAS_NO_SEGMENTS("navRefSeqName", "navAlignmentName", "featureName"),
		NAVIGATION_ALIGNMENT_MEMBER_DOES_NOT_COVER_FEATURE("navAlignmentName", "memberSourceName", "memberSequenceID", "featureName");
		
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
