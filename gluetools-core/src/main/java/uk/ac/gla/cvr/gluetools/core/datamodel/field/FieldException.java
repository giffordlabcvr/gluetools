package uk.ac.gla.cvr.gluetools.core.datamodel.field;

import uk.ac.gla.cvr.gluetools.core.GlueException;

@SuppressWarnings("serial")
public class FieldException extends GlueException {

	public enum Code implements GlueErrorCode {
		INCORRECT_VALUE_FORMAT("input", "fieldClass", "errorTxt"), 
		INVALID_FIELD("invalidField", "validFields");
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}
	
	public FieldException(GlueErrorCode code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public FieldException(Throwable cause, GlueErrorCode code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
