package uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class AlignmentMemberException extends GlueException {

	public enum Code implements GlueErrorCode {

		CANNOT_DETERMINE_TARGET_REFERENCE_FROM_ALIGNMENT_MEMBER("alignmentName","sourceName","sequenceID");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	protected AlignmentMemberException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

	public AlignmentMemberException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}
	
}
