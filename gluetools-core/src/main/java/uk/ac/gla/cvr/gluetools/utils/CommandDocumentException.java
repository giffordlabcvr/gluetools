package uk.ac.gla.cvr.gluetools.utils;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class CommandDocumentException extends GlueException {

	
	public enum Code implements GlueErrorCode {

		XML_TO_COMMAND_DOCUMENT_ERROR("errorTxt"),
		JSON_TO_COMMAND_DOCUMENT_ERROR("errorTxt"),
		COMMAND_DOCUMENT_TO_JSON_ERROR("errorTxt");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public CommandDocumentException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public CommandDocumentException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
	
}
