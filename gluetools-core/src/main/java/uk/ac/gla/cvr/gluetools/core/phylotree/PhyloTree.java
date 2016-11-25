package uk.ac.gla.cvr.gluetools.core.phylotree;


public class PhyloTree extends PhyloObject<PhyloTree> {

	private PhyloSubtree<?> root;
	
	public void accept(PhyloTreeVisitor visitor) {
		visitor.preVisitTree(this);
		root.accept(visitor);
		visitor.postVisitTree(this);
	}

	public PhyloSubtree<?> getRoot() {
		return root;
	}

	public void setRoot(PhyloSubtree<?> root) {
		if(this.root != null) {
			this.root.setTree(null);
		}
		this.root = root;
		if(root != null) {
			root.setTree(this);
			root.setParentPhyloBranch(null);
		}
	}

	@Override
	public PhyloTree clone() {
		PhyloTree phyloTree = new PhyloTree();
		copyPropertiesTo(phyloTree);
		return phyloTree;
	}
	
}
