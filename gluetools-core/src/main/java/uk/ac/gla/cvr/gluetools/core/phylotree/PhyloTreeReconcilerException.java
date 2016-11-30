package uk.ac.gla.cvr.gluetools.core.phylotree;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class PhyloTreeReconcilerException extends GlueException {

public enum Code implements GlueErrorCode {
		
		PHYLO_TREE_RECONCILER_ERROR("errorTxt");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public PhyloTreeReconcilerException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public PhyloTreeReconcilerException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
}
