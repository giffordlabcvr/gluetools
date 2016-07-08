package uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree;

public class PhyloBranch {

	private Double length;
	private PhyloSubtree subtree;
	
	public Double getLength() {
		return length;
	}

	public void setLength(Double length) {
		this.length = length;
	}

	public PhyloSubtree getSubtree() {
		return subtree;
	}

	public void setSubtree(PhyloSubtree subtree) {
		this.subtree = subtree;
	}
	
	public void accept(int branchIndex, PhyloTreeVisitor visitor) {
		visitor.preVisitBranch(branchIndex, this);
		subtree.accept(visitor);
		visitor.postVisitBranch(branchIndex, this);
	}

}
