package uk.ac.gla.cvr.gluetools.core.newick;

import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloInternal;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;

public interface NewickGenerator extends Plugin {

	/* floating point number associated with branches */
	public default String generateBranchLength(PhyloBranch phyloBranch) {
		return (String) phyloBranch.ensureUserData().get("length");
	}

	/* main string associated with leaf nodes */
	public default String generateLeafName(PhyloLeaf phyloLeaf) {
		return (String) phyloLeaf.ensureUserData().get("name");
	}

	/* main string associated with internal nodes. Appears after ")" */
	public default String generateInternalName(PhyloInternal phyloInternal) {
		return (String) phyloInternal.ensureUserData().get("name");
	}

	/* optional string within [] associated with branches */
	public default String generateBranchComment(PhyloBranch phyloBranch) {
		return (String) phyloBranch.ensureUserData().get("comment");
	}

	/* optional string within {}, associated with branches */
	public default String generateBranchLabel(PhyloBranch phyloBranch) {
		return (String) phyloBranch.ensureUserData().get("label");
	}

}
