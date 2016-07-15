package uk.ac.gla.cvr.gluetools.core.treerenderer;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class TreeRendererException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		ALIGNMENT_HAS_NO_MEMBERS_OR_CHILDREN("alignmentName");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public TreeRendererException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public TreeRendererException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

	
}
