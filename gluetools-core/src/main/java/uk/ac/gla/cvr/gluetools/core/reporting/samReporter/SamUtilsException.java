package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class SamUtilsException extends GlueException {

	
	public enum Code implements GlueErrorCode {

		ALIGNMENT_LINE_USES_EQUALS(), 
		ALIGNMENT_LINE_USES_UNKNOWN_CHARACTER("readChar", "ascii"),
		SAM_BAM_FILE_HAS_ZERO_REFERENCES("fileName"),
		SAM_BAM_FILE_HAS_MULTIPLE_REFERENCES("fileName"),
		SAM_BAM_FILE_MISSING_REFERENCE("fileName", "samRefName"),
		SAM_FORMAT_ERROR("errorTxt"),
		;

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	protected SamUtilsException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

	public SamUtilsException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}
	
}
