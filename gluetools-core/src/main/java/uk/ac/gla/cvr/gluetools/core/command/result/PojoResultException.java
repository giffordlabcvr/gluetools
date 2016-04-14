package uk.ac.gla.cvr.gluetools.core.command.result;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class PojoResultException extends GlueException {

public enum Code implements GlueErrorCode {
		
		POJO_PROPERTY_READ_ERROR("errorText"),
		CLASS_NOT_ANNOTATED("className"),
		POJO_FIELD_INCORRECT_MODIFIERS("fieldName", "className");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}

	}
	
	public PojoResultException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public PojoResultException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
}
