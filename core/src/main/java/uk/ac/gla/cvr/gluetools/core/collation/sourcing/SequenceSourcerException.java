package uk.ac.gla.cvr.gluetools.core.collation.sourcing;

import uk.ac.gla.cvr.gluetools.core.GlueException;

@SuppressWarnings("serial")
public class SequenceSourcerException extends GlueException {

	public enum Code implements GlueErrorCode {
		IO_ERROR, 
		FORMATTING_ERROR,
		SEARCH_ERROR,
		PROTOCOL_ERROR,
		CANNOT_PROCESS_SEQUENCE_FORMAT, 
		INSUFFICIENT_SEQUENCES_RETURNED
	}

	public SequenceSourcerException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public SequenceSourcerException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
	

}
