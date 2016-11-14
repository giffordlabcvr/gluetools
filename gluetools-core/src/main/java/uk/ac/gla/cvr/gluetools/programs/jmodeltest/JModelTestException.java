package uk.ac.gla.cvr.gluetools.programs.jmodeltest;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class JModelTestException extends GlueException {

	public enum Code implements GlueErrorCode {

		JMODELTEST_CONFIG_EXCEPTION("errorTxt"), 
		JMODELTEST_DATA_EXCEPTION("errorTxt"), 
		JMODELTEST_FILE_EXCEPTION("errorTxt"),
		JMODELTEST_PROCESS_EXCEPTION("errorTxt");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}
	
	public JModelTestException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public JModelTestException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

}
