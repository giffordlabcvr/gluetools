package uk.ac.gla.cvr.gluetools.core.command.project;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class ProjectModeCommandException extends GlueException {

	public enum Code implements GlueErrorCode {
		INVALID_PROPERTY("invalidProperty", "validProperties", "tableName"),
		INCOMPATIBLE_TYPES_FOR_COPY("fromFieldName", "toFieldName"), 
		NO_SUCH_MODIFIABLE_PROPERTY("tableName", "fieldName"), 
		NO_SUCH_PROPERTY("tableName", "fieldName"),
		NO_SUCH_TABLE("tableName"),
		NO_SUCH_CUSTOM_TABLE("tableName"),
		INCORRECT_FIELD_TYPE("tableName", "fieldName", "requiredFieldType", "actualFieldType");
		
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
