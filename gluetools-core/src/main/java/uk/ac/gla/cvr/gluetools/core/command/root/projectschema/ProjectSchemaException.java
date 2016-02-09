package uk.ac.gla.cvr.gluetools.core.command.root.projectschema;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class ProjectSchemaException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		NOT_A_CONFIGURABLE_TABLE("tableName");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}

	}
	
	public ProjectSchemaException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public ProjectSchemaException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
	
}
