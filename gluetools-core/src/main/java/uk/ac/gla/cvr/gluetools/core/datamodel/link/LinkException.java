package uk.ac.gla.cvr.gluetools.core.datamodel.link;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class LinkException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		LINK_MULTIPLICITY_ERROR("tableName", "linkName", "errorTxt");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}
	
	public LinkException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public LinkException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
}
