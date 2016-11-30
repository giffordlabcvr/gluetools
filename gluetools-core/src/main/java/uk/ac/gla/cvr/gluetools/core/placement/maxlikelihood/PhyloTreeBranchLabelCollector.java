package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeVisitor;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodPlacerException.Code;

// collects branch labels from the JPlace tree and stores them in a map from label to branch object
public class PhyloTreeBranchLabelCollector implements PhyloTreeVisitor {

	private Map<Integer, PhyloBranch> labelToJPlaceBranch = new LinkedHashMap<Integer, PhyloBranch>();
	
	@Override
	public void preVisitBranch(int branchIndex, PhyloBranch jPlacePhyloBranch) {
		String branchLabelString = jPlacePhyloBranch.getBranchLabel();
		if(branchLabelString == null) {
			throw new MaxLikelihoodPlacerException(Code.JPLACE_BRANCH_LABEL_ERROR, "Expected jPlace branch to have a branch label (within '{' and '}')");
		}
		Integer branchLabel = null;
		try {
			branchLabel = Integer.parseInt(branchLabelString);
		} catch(NumberFormatException nfe) {
			throw new MaxLikelihoodPlacerException(Code.JPLACE_BRANCH_LABEL_ERROR, "Expected jPlace branch to have an integer branch label");
		}
		labelToJPlaceBranch.put(branchLabel, jPlacePhyloBranch);
	}

	public Map<Integer, PhyloBranch> getLabelToJPlaceBranch() {
		return labelToJPlaceBranch;
	}
}