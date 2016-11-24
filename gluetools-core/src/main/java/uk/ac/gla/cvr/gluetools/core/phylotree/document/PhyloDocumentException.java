package uk.ac.gla.cvr.gluetools.core.phylotree.document;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class PhyloDocumentException extends GlueException {

	public enum Code implements GlueErrorCode {
		ILLEGAL_USER_DATA_VALUE("fieldName","className"),
		UNKNOWN_KEY("keyName"),
		FORMAT_ERROR("errorTxt");
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
		
	}
	
	public PhyloDocumentException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public PhyloDocumentException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
}
