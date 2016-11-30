package uk.ac.gla.cvr.gluetools.core.document.pojo;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class PojoDocumentException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		POJO_PROPERTY_READ_ERROR("errorText"),
		CLASS_NOT_ANNOTATED("className"),
		POJO_FIELD_INCORRECT_MODIFIERS("fieldName", "className"),
		POJO_CREATION_FAILED("className", "errorText"),
		DOCUMENT_TO_POJO_FAILED("errorText"),
		POJO_ANNOTATION_ERROR("errorText");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}

	}
	
	public PojoDocumentException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public PojoDocumentException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
}
