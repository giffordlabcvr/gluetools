package uk.ac.gla.cvr.gluetools.core.samFileGenerator;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class SamFileGeneratorException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		IO_ERROR("errorTxt"),
		CONFIG_ERROR("errorTxt");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}
	
	public SamFileGeneratorException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public SamFileGeneratorException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
}
