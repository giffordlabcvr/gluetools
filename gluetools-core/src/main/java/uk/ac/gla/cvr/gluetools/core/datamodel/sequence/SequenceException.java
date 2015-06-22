package uk.ac.gla.cvr.gluetools.core.datamodel.sequence;

import uk.ac.gla.cvr.gluetools.core.GlueException;

@SuppressWarnings("serial")
public class SequenceException extends GlueException {

	public enum Code implements GlueErrorCode {
		MALFORMED_XML("errorTxt"), 
		CREATE_FROM_FILE_FAILED("file"),
		UNKNOWN_SEQUENCE_FORMAT("unknownFormat"),
		NO_DATA_PROVIDED(),
		BASE_64_FORMAT_EXCEPTION("errorText");
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}
	
	public SequenceException(GlueErrorCode code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public SequenceException(Throwable cause, GlueErrorCode code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
