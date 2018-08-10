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
package uk.ac.gla.cvr.gluetools.core.phylotree;

import java.util.ArrayList;
import java.util.List;

public class PhyloInternal extends PhyloSubtree<PhyloInternal> {

	private List<PhyloBranch> branches = new ArrayList<PhyloBranch>();
	
	public void addBranch(PhyloBranch branch) {
		branch.setChildBranchIndex(branches.size());
		branches.add(branch);
		if(branch.getParentPhyloInternal() != null) {
			throw new RuntimeException("PhyloBranch must be removed from existing parent before it can be added to a new parent.");
		}
		branch.setParentPhyloInternal(this);
	}
	
	public void removeBranch(PhyloBranch branch) {
		boolean removed = branches.remove(branch);
		if(!removed) {
			throw new RuntimeException("Attempt to remove PhyloBranch object which was not member of branches list");
		}
		if(branch.getParentPhyloInternal() != this) {
			throw new RuntimeException("Attempt to remove PhyloBranch object which did not have correct parent internal");
		}
		branch.setParentPhyloInternal(null);
		for(int i = 0; i < branches.size(); i++) {
			branches.get(i).setChildBranchIndex(i);
		}
	}
	
	@Override
	public void accept(PhyloTreeVisitor visitor) {
		visitor.preVisitInternal(this);
		int numBranches = branches.size();
		for(int i = 0; i < numBranches; i++) {
			branches.get(i).accept(i, visitor);
		}
		visitor.postVisitInternal(this);
	}

	public List<PhyloBranch> getBranches() { // should not be altered after it's retrieved.
		return branches;
	}

	// introduce arbitrary internal nodes such that every internal 
	// node descendent from this node (inclusive) has at most 2 branches.
	public void forceBifurcating() {
		List<PhyloBranch> initialBranches = new ArrayList<PhyloBranch>(getBranches());
		if(initialBranches.size() > 2) {
			List<PhyloBranch> branchesToMove = new ArrayList<PhyloBranch>();
			for(int i = 1; i < initialBranches.size(); i++) {
				PhyloBranch branchToMove = initialBranches.get(i);
				removeBranch(branchToMove);
				branchesToMove.add(branchToMove);
			}
			PhyloInternal currentPhyloInternal = this;
			for(int j = 0; j < branchesToMove.size() - 1; j++) {
				PhyloBranch newBranch = new PhyloBranch();
				currentPhyloInternal.addBranch(newBranch);
				PhyloInternal newPhyloInternal = new PhyloInternal();
				newBranch.setSubtree(newPhyloInternal);
				PhyloBranch branchToMove = branchesToMove.get(j);
				newPhyloInternal.addBranch(branchToMove);
				currentPhyloInternal = newPhyloInternal;
			}
			currentPhyloInternal.addBranch(branchesToMove.get(branchesToMove.size()-1));
		}		
	}

	@Override
	public PhyloInternal clone() {
		PhyloInternal phyloInternal = new PhyloInternal();
		copyPropertiesTo(phyloInternal);
		return phyloInternal;
	}

	@Override
	public List<PhyloSubtree<?>> getNeighbours() {
		List<PhyloSubtree<?>> neighbours = super.getNeighbours();
		getBranches().stream().forEach(branch -> neighbours.add(branch.getSubtree()));
		return neighbours;
	}
	
	@Override
	public List<PhyloBranch> getNeighbourBranches() {
		List<PhyloBranch> neighbourBranches = super.getNeighbourBranches();
		neighbourBranches.addAll(getBranches());
		return neighbourBranches;
	}
}
