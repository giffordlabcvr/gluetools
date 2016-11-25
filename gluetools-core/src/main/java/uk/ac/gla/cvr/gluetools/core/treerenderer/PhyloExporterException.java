package uk.ac.gla.cvr.gluetools.core.treerenderer;

import uk.ac.gla.cvr.gluetools.core.GlueException;

public class PhyloExporterException extends GlueException {

	public enum Code implements GlueErrorCode {
		
		ALIGNMENT_HAS_NO_MEMBERS_OR_CHILDREN("alignmentName"),
		PHYLOGENY_REFERENCES_NON_CHILD_ALIGNMENT("referencingAlmt", "phyloField", "childAlmt");
		private String[] argNames;
		private Code(String... argNames) {
			this.argNames = argNames;
		}
		@Override
		public String[] getArgNames() {
			return argNames;
		}
	}

	public PhyloExporterException(Code code, Object... errorArgs) {
		super(code, errorArgs);
	}

	public PhyloExporterException(Throwable cause, Code code,
			Object... errorArgs) {
		super(cause, code, errorArgs);
	}

	
}
