package uk.ac.gla.cvr.gluetools.programs.mafft;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class MafftException extends GlueException {

	public enum Code implements GlueErrorCode {

		MAFFT_CONFIG_EXCEPTION("errorTxt"), 
		MAFFT_DATA_EXCEPTION("errorTxt"), 
		MAFFT_FILE_EXCEPTION("errorTxt"),
		MAFFT_PROCESS_EXCEPTION("errorTxt"),
		MAFFT_EXECUTION_EXCEPTION("errorTxt");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public MafftException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public MafftException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
}
