package uk.ac.gla.cvr.gluetools.core.collation.referenceBuilder;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class GbRefBuilderException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		NOT_GENBANK_XML_FORMAT("sourceName", "sequenceID");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}

	}
	
	public GbRefBuilderException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public GbRefBuilderException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}


}
