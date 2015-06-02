package uk.ac.gla.cvr.gluetools.core.datafield.populator;

import uk.ac.gla.cvr.gluetools.core.GlueException;

@SuppressWarnings("serial")
public class DataFieldPopulatorException extends GlueException {

	public enum Code implements GlueErrorCode {
		
	}

	public DataFieldPopulatorException(GlueErrorCode code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public DataFieldPopulatorException(Throwable cause, GlueErrorCode code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
	
	
}
