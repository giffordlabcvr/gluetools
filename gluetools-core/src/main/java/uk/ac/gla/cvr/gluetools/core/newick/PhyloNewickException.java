package uk.ac.gla.cvr.gluetools.core.newick;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class PhyloNewickException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		ILLEGAL_NEWICK_NODE_NAME("nodeName"),
		SYNTAX_ERROR("characterPosition"), 
		UNEXPECTED_NEWICK_TOKEN("token", "characterPosition"),
		PARSE_ERROR("tokenType", "tokenValue", "position"),
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

	public PhyloNewickException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public PhyloNewickException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

	
}
