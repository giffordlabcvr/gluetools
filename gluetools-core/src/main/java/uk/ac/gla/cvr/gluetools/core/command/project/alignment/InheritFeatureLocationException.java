package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class InheritFeatureLocationException extends GlueException {


	public enum Code implements GlueErrorCode {
		NO_PARENT_ALIGNMENT("almtName");
		
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
