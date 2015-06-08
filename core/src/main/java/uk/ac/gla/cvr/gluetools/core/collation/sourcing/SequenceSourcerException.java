package uk.ac.gla.cvr.gluetools.core.collation.sourcing;

import uk.ac.gla.cvr.gluetools.core.GlueException;

@SuppressWarnings("serial")
public class SequenceSourcerException extends GlueException {

	public enum Code implements GlueErrorCode {
		IO_ERROR("requestName", "errorTxt"), 
		FORMATTING_ERROR("requestName", "errorTxt"),
		SEARCH_ERROR("errorTxt"),
		PROTOCOL_ERROR("requestName", "errorTxt"),
		CANNOT_PROCESS_SEQUENCE_FORMAT("formatName"), 
		INSUFFICIENT_SEQUENCES_RETURNED();
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public SequenceSourcerException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public SequenceSourcerException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
	

}
