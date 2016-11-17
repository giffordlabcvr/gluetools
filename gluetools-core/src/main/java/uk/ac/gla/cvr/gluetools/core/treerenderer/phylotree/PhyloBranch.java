package uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree;

import java.math.BigDecimal;
import java.util.LinkedList;

public class PhyloBranch extends PhyloObject<PhyloBranch> {

	private PhyloInternal parentPhyloInternal; // phyloInternal for now (later may allow PhlyoTree?)
	private PhyloSubtree<?> subtree;
	private BigDecimal length;
	private String comment;
	private Integer branchLabel; // jPlace format extension
	
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

	
	public PhyloInternal reroot(BigDecimal rootPointDistance) {
		if(rootPointDistance.compareTo(BigDecimal.valueOf(0.0)) < 0 
				|| rootPointDistance.compareTo(getLength()) > 0) {
			throw new RuntimeException("Illegal root point distance");
		}
		PhyloTree rerootedTree = getTree().clone();
		PhyloInternal rerootedInternal = new PhyloInternal();
		rerootedTree.setRoot(rerootedInternal);
		
		LinkedList<RerootTask<?>> taskQueue = new LinkedList<RerootTask<?>>();
		addBranchToTaskQueue(this, taskQueue);
		while(!taskQueue.isEmpty()) {
			
		}
		
		return rerootedInternal;
	}

	private void addBranchToTaskQueue(PhyloBranch originalBranch, LinkedList<RerootTask<?>> taskQueue) {
		PhyloBranch clonedBranch = originalBranch.clone();
	}
	private void addLeafToTaskQueue(PhyloLeaf originalLeaf, LinkedList<RerootTask<?>> taskQueue) {
		
	}
	private void addInternalToTaskQueue(PhyloInternal originalInternal, LinkedList<RerootTask<?>> taskQueue) {
		
	}
	
	private class RerootTask<D extends PhyloObject<?>> {
		RerootDirection rerootDirection;
		D original; 
		D cloned; 
	}
	
	private enum RerootDirection {
		FROM_LEFT,
		FROM_RIGHT,
		FROM_PARENT;
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
