package uk.ac.gla.cvr.gluetools.core.collation.populating.textfile;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class TextFilePopulatorException extends GlueException {

	public enum Code implements GlueErrorCode {
		HEADER_NOT_FOUND("header"),
		NO_SEQUENCE_FOUND("combinedWhereClause"),
		MULTIPLE_SEQUENCES_FOUND("combinedWhereClause"), 
		NULL_IDENTIFIER("fieldName"), 
		NO_SUCH_PROPERTY("property", "definedProperties");
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public TextFilePopulatorException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public TextFilePopulatorException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
	
}
