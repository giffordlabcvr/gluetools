package uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree;

import java.util.ArrayList;
import java.util.List;

public class PhyloInternal extends PhyloSubtree {

	private List<PhyloBranch> branches = new ArrayList<PhyloBranch>();
	
	public void addBranch(PhyloBranch branch) {
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
	}
	
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

	
}
