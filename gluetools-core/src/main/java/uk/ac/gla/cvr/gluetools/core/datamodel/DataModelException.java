package uk.ac.gla.cvr.gluetools.core.datamodel;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class DataModelException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		OBJECT_NOT_FOUND("entityName", "idMap"),
		OBJECT_ALREADY_EXISTS("entityName", "idMap"),
		MULTIPLE_OBJECTS_FOUND("entityName", "idMap"), 
		DELETE_DENIED("entityName", "idMap", "relationship"),
		EXPRESSION_ERROR("expression", "errorTxt");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public DataModelException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public DataModelException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
