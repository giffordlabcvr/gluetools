package uk.ac.gla.cvr.gluetools.core.validation;

import uk.ac.gla.cvr.gluetools.core.GlueException;

@SuppressWarnings("serial")
public class ValidateException extends GlueException {

	public enum Code implements GlueErrorCode {
		VALIDATION_ERROR("objectPath", "class", "code", "errorTxt");
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
		
	}

	public ValidateException(String objectPath, Throwable cause) {
		super(cause, Code.VALIDATION_ERROR, objectPath, cause.getClass().getSimpleName(), 
				(cause instanceof GlueException) ? ((GlueException) cause).getCode().name() : null, cause.getLocalizedMessage() );
	}

}
