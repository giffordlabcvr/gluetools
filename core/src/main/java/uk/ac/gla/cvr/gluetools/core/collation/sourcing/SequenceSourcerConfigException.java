package uk.ac.gla.cvr.gluetools.core.collation.sourcing;

import uk.ac.gla.cvr.gluetools.core.GlueException;

@SuppressWarnings("serial")
public class SequenceSourcerConfigException extends GlueException {

	public enum Code implements GlueErrorCode {
		REQUIRED_ELEMENT_MISSING, 
		CONFIG_VALUE_FORMAT_ERROR
	}
	
	public SequenceSourcerConfigException(Code code,
			Object... errorArgs) {
		super(code, errorArgs);
	}

	public SequenceSourcerConfigException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
