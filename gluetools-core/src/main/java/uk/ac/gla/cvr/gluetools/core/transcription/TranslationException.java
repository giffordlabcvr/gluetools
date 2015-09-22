package uk.ac.gla.cvr.gluetools.core.transcription;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class TranslationException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		UNKNOWN_TRANSLATION_TYPE("unknownTranscriptionType");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public TranslationException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public TranslationException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}