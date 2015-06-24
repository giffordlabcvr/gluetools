package uk.ac.gla.cvr.gluetools.core.datamodel.builder;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class ModelBuilderException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		PROJECT_TABLE_MISSING("projectName", "tableName");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public ModelBuilderException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public ModelBuilderException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
