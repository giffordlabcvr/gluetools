package uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree;

public interface PhyloTreeVisitor {

	public default void preVisitTree(PhyloTree phyloTree) {}
	public default void postVisitTree(PhyloTree phyloTree) {}

	public default void preVisitInternal(PhyloInternal phyloInternal) {}
	public default void postVisitInternal(PhyloInternal phyloInternal) {}

	public default void visitLeaf(PhyloLeaf phyloLeaf) {}

	public default void preVisitBranch(int branchIndex, PhyloBranch phyloBranch) {}

	public default void postVisitBranch(int branchIndex, PhyloBranch phyloBranch) {}


	
}
