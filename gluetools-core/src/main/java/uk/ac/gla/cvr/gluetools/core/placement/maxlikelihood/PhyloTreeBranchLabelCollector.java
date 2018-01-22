/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
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