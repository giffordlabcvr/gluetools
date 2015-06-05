package uk.ac.gla.cvr.gluetools.core.collation.sequence;

import uk.ac.gla.cvr.gluetools.core.GlueException;

@SuppressWarnings("serial")
public class SequenceFormatException extends GlueException {

	public enum Code implements GlueErrorCode {
		MALFORMED_XML
	}
	
	public SequenceFormatException(GlueErrorCode code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public SequenceFormatException(Throwable cause, GlueErrorCode code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
