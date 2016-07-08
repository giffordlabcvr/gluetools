package uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree;

public class PhyloTree {

	private PhyloSubtree root;
	
	public void accept(PhyloTreeVisitor visitor) {
		visitor.preVisitTree(this);
		root.accept(visitor);
		visitor.postVisitTree(this);
	}

	public PhyloSubtree getRoot() {
		return root;
	}

	public void setRoot(PhyloSubtree root) {
		this.root = root;
	}
}
