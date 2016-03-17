package uk.ac.gla.cvr.gluetools.core.curation.aligners.compound;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class CompoundAlignerException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		ELEMENT_IS_NOT_AN_ALIGNER("elementName");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}

	}
	
	public CompoundAlignerException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public CompoundAlignerException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
}
