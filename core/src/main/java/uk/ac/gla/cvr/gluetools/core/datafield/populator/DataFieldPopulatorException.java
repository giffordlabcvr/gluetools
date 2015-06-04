package uk.ac.gla.cvr.gluetools.core.datafield.populator;

import uk.ac.gla.cvr.gluetools.core.GlueException;

@SuppressWarnings("serial")
public class DataFieldPopulatorException extends GlueException {

	public enum Code implements GlueErrorCode {
		INCORRECT_VALUE_FORMAT, NO_SUCH_FIELD, POPULATOR_RULE_FAILED, POPULATOR_CHILD_RULE_FAILED
		
	}

	public DataFieldPopulatorException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public DataFieldPopulatorException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
	
	
}
