package uk.ac.gla.cvr.gluetools.core.phylotree;


public class PhyloBranch extends PhyloObject<PhyloBranch> {

	private PhyloInternal parentPhyloInternal; // phyloInternal for now (later may allow PhlyoTree?)
	private PhyloSubtree<?> subtree;
	private Double length;
	private String comment;
	private Integer branchLabel; // jPlace format extension
	private int childBranchIndex;
	
	public int getChildBranchIndex() {
		return childBranchIndex;
	}

	public void setChildBranchIndex(int childBranchIndex) {
		this.childBranchIndex = childBranchIndex;
	}

	public PhyloSubtree<?> getSubtree() {
		return subtree;
	}

	public Integer getBranchLabel() {
		return branchLabel;
	}

	public void setBranchLabel(Integer branchLabel) {
		this.branchLabel = branchLabel;
	}

	public void setSubtree(PhyloSubtree<?> subtree) {
		this.subtree = subtree;
		subtree.setParentPhyloBranch(this);
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
	
	public PhyloInternal getParentPhyloInternal() {
		return parentPhyloInternal;
	}

	void setParentPhyloInternal(PhyloInternal parentPhyloInternal) {
		this.parentPhyloInternal = parentPhyloInternal;
	}

	public void accept(int branchIndex, PhyloTreeVisitor visitor) {
		visitor.preVisitBranch(branchIndex, this);
		subtree.accept(visitor);
		visitor.postVisitBranch(branchIndex, this);
	}

	
	
	@Override
	public PhyloBranch clone() {
		PhyloBranch phyloBranch = new PhyloBranch();
		copyPropertiesTo(phyloBranch);
		phyloBranch.setBranchLabel(getBranchLabel());
		phyloBranch.setComment(getComment());
		phyloBranch.setLength(getLength());
		return phyloBranch;
	}

	public PhyloTree getTree() {
		return getParentPhyloInternal().getTree();
	}

	
	
	
}
