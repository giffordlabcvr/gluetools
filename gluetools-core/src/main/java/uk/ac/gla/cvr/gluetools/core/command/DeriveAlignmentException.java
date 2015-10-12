package uk.ac.gla.cvr.gluetools.core.command;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class DeriveAlignmentException extends GlueException {

	public enum Code implements GlueErrorCode {
		TARGET_ALIGNMENT_IS_UNCONSTRAINED("targetAlmtName"),
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
	
	public DeriveAlignmentException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public DeriveAlignmentException(Throwable cause, Code code, Object... errorArgs) {
		super(cause, code, errorArgs);
	}
}
