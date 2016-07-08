package uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree;

import java.util.ArrayList;
import java.util.List;

public class PhyloInternal extends PhyloSubtree {

	private List<PhyloBranch> branches = new ArrayList<PhyloBranch>();
	
	public void addBranch(PhyloBranch branch) {
		branches.add(branch);
	}
	
	public void accept(PhyloTreeVisitor visitor) {
		visitor.preVisitInternal(this);
		int numBranches = branches.size();
		for(int i = 0; i < numBranches; i++) {
			branches.get(i).accept(i, visitor);
		}
		visitor.postVisitInternal(this);
	}

	public List<PhyloBranch> getBranches() {
		return branches;
	}

	public void setBranches(List<PhyloBranch> branches) {
		this.branches = branches;
	}

	
}
