package uk.ac.gla.cvr.gluetools.core.datamodel.refSequence;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class ReferenceSequenceException extends GlueException {

	public enum Code implements GlueErrorCode {

		REFERENCE_SEQUENCE_MEMBER_OF_MULTIPLE_TIP_ALIGNMENTS("refName"),
		REFERENCE_SEQUENCE_MEMBER_OF_NO_TIP_ALIGNMENTS("refName"),
		REFERENCE_SEQUENCE_NOT_MEMBER_OF_CONSTRAINED_ALIGNMENT("refName", "almtName"),
		REFERENCE_SEQUENCE_DOES_NOT_CONSTRAIN_ANY_ALIGNMENT("refName"),
		REFERENCE_SEQUENCE_CONSTRAINS_MULTIPLE_ALIGNMENTS("refName");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}
	
	
	public ReferenceSequenceException(GlueErrorCode code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public ReferenceSequenceException(Throwable cause, GlueErrorCode code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}


	
}
