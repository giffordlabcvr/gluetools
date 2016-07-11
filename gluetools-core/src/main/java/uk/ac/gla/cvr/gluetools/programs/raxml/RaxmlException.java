package uk.ac.gla.cvr.gluetools.programs.raxml;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class RaxmlException extends GlueException {

	public enum Code implements GlueErrorCode {

		RAXML_CONFIG_EXCEPTION("errorTxt"), 
		RAXML_DATA_EXCEPTION("errorTxt"), 
		RAXML_FILE_EXCEPTION("errorTxt"),
		RAXML_PROCESS_EXCEPTION("errorTxt");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public RaxmlException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public RaxmlException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
}
