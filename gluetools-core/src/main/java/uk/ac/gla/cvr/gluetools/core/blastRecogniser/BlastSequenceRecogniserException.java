package uk.ac.gla.cvr.gluetools.core.blastRecogniser;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class BlastSequenceRecogniserException extends GlueException {

	public enum Code implements GlueErrorCode {

		NO_SUCH_REFERENCE_SEQUENCE("refName"),
		CATEGORY_USES_UNKNOWN_REFERENCE("categoryId", "refName"),
		CATEGORY_REFERENCES_OVERLAP("categoryId1", "categoryId2", "refName"),
		NO_CATEGORY_FOR_REFERENCE("refName");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	protected BlastSequenceRecogniserException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

	public BlastSequenceRecogniserException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}
	
	
}
