package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class InheritFeatureLocationException extends GlueException {


	public enum Code implements GlueErrorCode {
		NOT_MEMBER_OF_ALIGNMENT("refSeqName", "almtName"),
		PARENT_ALIGNMENT_IS_UNCONSTRAINED("almtName");
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}
	
	public InheritFeatureLocationException(GlueErrorCode code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public InheritFeatureLocationException(Throwable cause, GlueErrorCode code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
}
