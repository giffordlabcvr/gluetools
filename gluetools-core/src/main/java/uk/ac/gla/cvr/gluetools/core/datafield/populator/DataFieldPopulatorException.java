package uk.ac.gla.cvr.gluetools.core.datafield.populator;

import uk.ac.gla.cvr.gluetools.core.GlueException;

@SuppressWarnings("serial")
public class DataFieldPopulatorException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		INCORRECT_VALUE_FORMAT("input", "fieldClass", "fieldName", "errorTxt"), 
		NO_SUCH_FIELD("fieldName", "projectID"), 
		POPULATOR_RULE_FAILED("errorTxt"), 
		POPULATOR_CHILD_RULE_FAILED("errorTxt");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}

	}

	public DataFieldPopulatorException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public DataFieldPopulatorException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
	
	
}
