package uk.ac.gla.cvr.gluetools.core.newick;

import uk.ac.gla.cvr.gluetools.core.newick.PhyloNewickException.Code;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;

public class NewickJPlaceToPhyloTreeParser extends NewickToPhyloTreeParser {

	public static final String J_PLACE_BRANCH_LABEL = "jPlaceBranchLabel";

	public NewickJPlaceToPhyloTreeParser() {
		super(new NewickInterpreter() {
			@Override
			public void parseBranchLabel(PhyloBranch phyloBranch, String label) {
				Integer branchLabel = null;
				try {
					branchLabel = Integer.parseInt(label);
				} catch(NumberFormatException nfe) {
					throw new PhyloNewickException(Code.FORMAT_ERROR, "Expected jPlace branch to have an integer branch label");
				}
				phyloBranch.ensureUserData().put(J_PLACE_BRANCH_LABEL, branchLabel);
			}
			
		});
	}

}
