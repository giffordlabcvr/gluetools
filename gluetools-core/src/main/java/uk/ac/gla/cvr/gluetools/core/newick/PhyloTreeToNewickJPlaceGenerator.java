package uk.ac.gla.cvr.gluetools.core.newick;

import uk.ac.gla.cvr.gluetools.core.newick.PhyloNewickException.Code;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;

public class PhyloTreeToNewickJPlaceGenerator extends PhyloTreeToNewickGenerator {

	public PhyloTreeToNewickJPlaceGenerator() {
		super(new NewickGenerator() {
			@Override
			public String generateBranchLabel(PhyloBranch phyloBranch) {
				Object branchLabel = phyloBranch.ensureUserData().get(NewickJPlaceToPhyloTreeParser.J_PLACE_BRANCH_LABEL);
				if(branchLabel == null) {
					throw new PhyloNewickException(Code.FORMAT_ERROR, "Expected jPlace PhyloBranch to have user data for key "+NewickJPlaceToPhyloTreeParser.J_PLACE_BRANCH_LABEL);
				}
				if(!(branchLabel instanceof Integer)) {
					throw new PhyloNewickException(Code.FORMAT_ERROR, "Expected jPlace PhyloBranch user data for key "+
							NewickJPlaceToPhyloTreeParser.J_PLACE_BRANCH_LABEL+" to be an integer");
				}
				return Integer.toString((Integer) branchLabel);
			}
		});
	}

}
