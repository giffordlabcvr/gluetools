package uk.ac.gla.cvr.gluetools.core.newick;

import java.math.BigDecimal;

import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloInternal;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;

public interface NewickInterpreter extends Plugin {

	/* floating point number associated with branches */
	public default void parseBranchLength(PhyloBranch phyloBranch, String branchLengthString) {
		Double branchLength = new BigDecimal(branchLengthString).doubleValue();
		phyloBranch.ensureUserData().put("length", branchLength);
	}

	/* main string associated with leaf nodes */
	public default void parseLeafName(PhyloLeaf phyloLeaf, String leafName) {
		phyloLeaf.ensureUserData().put("name", leafName);
	}

	/* main string associated with internal nodes. Appears after ")" */
	public default void parseInternalName(PhyloInternal phyloInternal, String internalName) {
		phyloInternal.ensureUserData().put("name", internalName);
	}

	/* optional string within [] associated with branches */
	public default void parseBranchComment(PhyloBranch phyloBranch, String comment) {
		phyloBranch.ensureUserData().put("comment", comment);
	}

	/* optional string within {}, associated with branches */
	public default void parseBranchLabel(PhyloBranch phyloBranch, String label) {
		phyloBranch.ensureUserData().put("label", label);
	}
	
}
