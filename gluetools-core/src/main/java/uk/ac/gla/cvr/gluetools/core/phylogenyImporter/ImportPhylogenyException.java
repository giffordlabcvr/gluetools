package uk.ac.gla.cvr.gluetools.core.phylogenyImporter;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class ImportPhylogenyException extends GlueException {

	public enum Code implements GlueErrorCode {
		MEMBER_LEAF_MISMATCH("errorTxt"),
		PHYLOGENY_INCONSISTENT("errorTxt");
		
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
		
	}
	
	public ImportPhylogenyException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public ImportPhylogenyException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

	
}
