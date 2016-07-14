package uk.ac.gla.cvr.gluetools.core.treetransformer;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class TreeTransformerException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		NO_SEQUENCES_MATCH_QUERY("sequenceQuery"),
		MULTIPLE_SEQUENCES_MATCH_QUERY("sequenceQuery");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public TreeTransformerException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public TreeTransformerException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
	
}
