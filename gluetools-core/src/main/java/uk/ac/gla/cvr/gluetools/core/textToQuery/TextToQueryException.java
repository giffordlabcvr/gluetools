package uk.ac.gla.cvr.gluetools.core.textToQuery;

import uk.ac.gla.cvr.gluetools.core.GlueException;

@SuppressWarnings("serial")
public class TextToQueryException extends GlueException {

	public enum Code implements GlueErrorCode {
		EXTRACTOR_FORMATTER_FAILED("description", "text"),
		MODULE_QUERY_OBJECT_TYPE_IS_INCORRECT("moduleName", "expectedQueryObjectType", "actualQueryObjectType");

		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
		
	}
	
	public TextToQueryException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public TextToQueryException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
