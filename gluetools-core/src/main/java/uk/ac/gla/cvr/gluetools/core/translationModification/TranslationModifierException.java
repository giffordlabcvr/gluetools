package uk.ac.gla.cvr.gluetools.core.translationModification;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class TranslationModifierException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		CONFIG_ERROR("errorTxt"),
		MODIFICATION_ERROR("errorTxt");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public TranslationModifierException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public TranslationModifierException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
}
