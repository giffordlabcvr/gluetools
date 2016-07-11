package uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree;

public class PhyloBranch {

	private PhyloSubtree subtree;
	private Double length;
	private String comment;
	
	public PhyloSubtree getSubtree() {
		return subtree;
	}

	public void setSubtree(PhyloSubtree subtree) {
		this.subtree = subtree;
	}
	
	public Double getLength() {
		return length;
	}

	public void setLength(Double length) {
		this.length = length;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public void accept(int branchIndex, PhyloTreeVisitor visitor) {
		visitor.preVisitBranch(branchIndex, this);
		subtree.accept(visitor);
		visitor.postVisitBranch(branchIndex, this);
	}

}
