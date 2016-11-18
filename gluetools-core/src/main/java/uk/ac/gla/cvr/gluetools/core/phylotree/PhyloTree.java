package uk.ac.gla.cvr.gluetools.core.phylotree;


public class PhyloTree extends PhyloObject<PhyloTree> {

	private PhyloSubtree<?> root;
	
	public void accept(PhyloTreeVisitor visitor) {
		visitor.preVisitTree(this);
		root.accept(visitor);
		visitor.postVisitTree(this);
	}

	public PhyloObject<?> getRoot() {
		return root;
	}

	public void setRoot(PhyloSubtree<?> root) {
		this.root = root;
		root.setTree(this);
	}

	@Override
	public PhyloTree clone() {
		PhyloTree phyloTree = new PhyloTree();
		copyPropertiesTo(phyloTree);
		return phyloTree;
	}
	
}
