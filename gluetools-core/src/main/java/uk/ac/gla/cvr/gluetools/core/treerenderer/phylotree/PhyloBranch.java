package uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree;

import java.math.BigDecimal;

public class PhyloBranch {

	private PhyloSubtree subtree;
	private BigDecimal length;
	private String comment;
	private Integer branchLabel; // jPlace format extension
	
	public PhyloSubtree getSubtree() {
		return subtree;
	}

	public Integer getBranchLabel() {
		return branchLabel;
	}

	public void setBranchLabel(Integer branchLabel) {
		this.branchLabel = branchLabel;
	}

	public void setSubtree(PhyloSubtree subtree) {
		this.subtree = subtree;
	}
	
	public BigDecimal getLength() {
		return length;
	}

	public void setLength(BigDecimal length) {
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
