package uk.ac.gla.cvr.gluetools.core.datamodel.alignment;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class AlignmentException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		PARENT_RELATIONSHIP_LOOP("alignmentNames"),
		REFERENCE_NOT_MEMBER_OF_PARENT("alignmentName", "parentAlignmentName", "referenceName"),
		REFERENCE_DOES_NOT_CONSTRAIN_ANCESTOR("referenceName", "alignmentName"),
		ALIGNMENT_NOT_CHILD_OF_PARENT("alignmentName", "parentAlignmentName"),
		ALIGNMENT_IS_UNCONSTRAINED("alignmentName"),
		CANNOT_SPECIFY_RECURSIVE_FOR_UNCONSTRAINED_ALIGNMENT("alignmentName"); 

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public AlignmentException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public AlignmentException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

	
}
