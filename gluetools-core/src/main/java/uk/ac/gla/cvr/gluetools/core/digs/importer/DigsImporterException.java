package uk.ac.gla.cvr.gluetools.core.digs.importer;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class DigsImporterException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		DIGS_DB_JDBC_URL_NOT_DEFINED(),
		DIGS_DB_JDBC_USER_NOT_DEFINED(),
		DIGS_DB_JDBC_PASSWORD_NOT_DEFINED(),
		DIGS_DB_ERROR("errorTxt"),
		EXPRESSION_ERROR("qualifier", "errorTxt"),
		ID_TEMPLATE_FAILED("errorTxt"),
		NO_SUCH_SEQUENCE_FIELD("fieldName");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}

	}
	
	public DigsImporterException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public DigsImporterException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
	
}
