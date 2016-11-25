package uk.ac.gla.cvr.gluetools.core.phylotree;

import java.math.BigDecimal;


public class PhyloBranch extends PhyloObject<PhyloBranch> {

	private PhyloInternal parentPhyloInternal; // phyloInternal for now (later may allow PhlyoTree?)
	private PhyloSubtree<?> subtree;
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

	public String getBranchLabel() {
		return (String) ensureUserData().get("label");
	}

	public void setBranchLabel(String branchLabel) {
		ensureUserData().put("label", branchLabel);		
	}

	public void setSubtree(PhyloSubtree<?> subtree) {
		this.subtree = subtree;
		subtree.setParentPhyloBranch(this);
	}
	
	public BigDecimal getLength() {
		return new BigDecimal((String) ensureUserData().get("length"));
	}

	public void setLength(BigDecimal length) {
		ensureUserData().put("length", length.toString());
	}

	public String getComment() {
		return (String) ensureUserData().get("comment");
	}

	public void setComment(String comment) {
		ensureUserData().put("comment", comment);		
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
		return phyloBranch;
	}

	public PhyloTree getTree() {
		return getParentPhyloInternal().getTree();
	}

	
	
	
}
