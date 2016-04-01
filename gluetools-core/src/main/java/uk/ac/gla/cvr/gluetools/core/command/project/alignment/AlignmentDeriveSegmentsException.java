package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class AlignmentDeriveSegmentsException extends GlueException {

	public enum Code implements GlueErrorCode {
		SOURCE_ALIGNMENT_IS_CONSTRAINED("sourceAlmtName"),
		REFERENCE_SEQUENCE_NOT_MEMBER_OF_SOURCE_ALIGNMENT("sourceAlmtName", "referenceName", "refSourceName", "refSeqSeqID", "targetAlmtName");
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}
	
	public AlignmentDeriveSegmentsException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public AlignmentDeriveSegmentsException(Throwable cause, Code code, Object... errorArgs) {
		super(cause, code, errorArgs);
	}
}
