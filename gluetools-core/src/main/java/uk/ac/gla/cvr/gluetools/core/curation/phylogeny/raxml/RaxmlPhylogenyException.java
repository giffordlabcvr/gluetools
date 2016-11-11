package uk.ac.gla.cvr.gluetools.core.curation.phylogeny.raxml;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class RaxmlPhylogenyException extends GlueException {

public enum Code implements GlueErrorCode {
		
		UNKNOWN_LEAF_NAME("leafId");

		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public RaxmlPhylogenyException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public RaxmlPhylogenyException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}
}
