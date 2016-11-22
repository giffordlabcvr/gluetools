package uk.ac.gla.cvr.gluetools.core.phylotree;

import java.util.ArrayList;
import java.util.List;

public class PhyloTreeSearchNode {

	// internal or leaf node that we are currently at.
	private PhyloSubtree<?> phyloSubtree;
	
	// branch from which we arrived at the current node.
	// may be null if we started from currentNode
	private PhyloBranch arrivalBranch;

	// start node
	public PhyloTreeSearchNode(PhyloSubtree<?> currentNode) {
		this(currentNode, null);
	}

	private PhyloTreeSearchNode(PhyloSubtree<?> currentNode,
			PhyloBranch arrivalBranch) {
		super();
		this.phyloSubtree = currentNode;
		this.arrivalBranch = arrivalBranch;
	}

	public boolean arrivedFromParent() {
		if(arrivalBranch != null && arrivalBranch.getSubtree() == phyloSubtree) {
			return true;
		}
		return false;
	}

	public Integer arrivedFromChild() {
		if(arrivalBranch != null && arrivalBranch.getParentPhyloInternal() == phyloSubtree) {
			return arrivalBranch.getChildBranchIndex();
		}
		return null;
	}
	
	public List<PhyloTreeSearchNode> neighbours() {
		List<PhyloTreeSearchNode> neighbours = new ArrayList<PhyloTreeSearchNode>();
		PhyloBranch parentPhyloBranch = phyloSubtree.getParentPhyloBranch();
		if(parentPhyloBranch != null && !arrivedFromParent()) {
			neighbours.add(new PhyloTreeSearchNode(parentPhyloBranch.getParentPhyloInternal(), parentPhyloBranch));
		}
		if(phyloSubtree instanceof PhyloInternal) {
			Integer arrivedFromChild = arrivedFromChild();
			for(PhyloBranch childBranch : ((PhyloInternal) phyloSubtree).getBranches()) {
				if(arrivedFromChild != null && arrivedFromChild.intValue() == childBranch.getChildBranchIndex()) {
					continue;
				}
				neighbours.add(new PhyloTreeSearchNode(childBranch.getSubtree(), childBranch));
			}
		}
		return neighbours;
	}

	public PhyloBranch getArrivalBranch() {
		return arrivalBranch;
	}

	public PhyloSubtree<?> getPhyloSubtree() {
		return phyloSubtree;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((arrivalBranch == null) ? 0 : arrivalBranch.hashCode());
		result = prime * result
				+ ((phyloSubtree == null) ? 0 : phyloSubtree.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PhyloTreeSearchNode other = (PhyloTreeSearchNode) obj;
		if (arrivalBranch == null) {
			if (other.arrivalBranch != null)
				return false;
		} else if (!arrivalBranch.equals(other.arrivalBranch))
			return false;
		if (phyloSubtree == null) {
			if (other.phyloSubtree != null)
				return false;
		} else if (!phyloSubtree.equals(other.phyloSubtree))
			return false;
		return true;
	}
	
	
	
}
