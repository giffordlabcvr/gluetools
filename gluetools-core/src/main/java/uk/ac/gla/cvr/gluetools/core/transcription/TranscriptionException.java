package uk.ac.gla.cvr.gluetools.core.transcription;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class TranscriptionException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		UNKNOWN_TRANSCRIPTION_TYPE("unknownTranscriptionType");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public TranscriptionException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public TranscriptionException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}