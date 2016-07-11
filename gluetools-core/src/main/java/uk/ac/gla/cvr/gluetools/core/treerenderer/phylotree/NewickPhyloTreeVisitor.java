package uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree;

import java.util.Optional;

public class NewickPhyloTreeVisitor implements PhyloTreeVisitor {
	
	private StringBuffer buf = new StringBuffer();
	
	public String getNewickString() {
		return buf.toString();
	}

	@Override
	public void preVisitTree(PhyloTree phyloTree) {
	}

	@Override
	public void postVisitTree(PhyloTree phyloTree) {
		buf.append(";");
	}

	@Override
	public void preVisitInternal(PhyloInternal phyloInternal) {
		buf.append("(");
	}

	@Override
	public void postVisitInternal(PhyloInternal phyloInternal) {
		buf.append(")");
		Optional.ofNullable(phyloInternal.getName()).ifPresent(name -> buf.append(name));
	}

	@Override
	public void visitLeaf(PhyloLeaf phyloLeaf) {
		Optional.ofNullable(phyloLeaf.getName()).ifPresent(name -> buf.append(name));
	}

	@Override
	public void preVisitBranch(int branchIndex, PhyloBranch phyloBranch) {
		if(branchIndex > 0) {
			buf.append(",");
		}
	}

	@Override
	public void postVisitBranch(int branchIndex, PhyloBranch phyloBranch) {
		Optional.ofNullable(phyloBranch.getLength()).ifPresent(length -> 
		buf.append(":").append(Double.toString(length)));
		Optional.ofNullable(phyloBranch.getComment()).ifPresent(comment -> 
		buf.append("[").append(comment).append("]"));
	}

	
}