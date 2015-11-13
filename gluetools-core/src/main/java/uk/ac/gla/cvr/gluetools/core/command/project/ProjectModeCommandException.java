package uk.ac.gla.cvr.gluetools.core.command.project;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class ProjectModeCommandException extends GlueException {

	public enum Code implements GlueErrorCode {
		INVALID_FIELD("invalidField", "validFields"),
		INCOMPATIBLE_TYPES_FOR_COPY("fromFieldName", "toFieldName");
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}
	
	public ProjectModeCommandException(GlueErrorCode code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public ProjectModeCommandException(Throwable cause, GlueErrorCode code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
	
}
