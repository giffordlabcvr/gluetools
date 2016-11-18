package uk.ac.gla.cvr.gluetools.core.phylotree;

public abstract class PhyloSubtree<C extends PhyloSubtree<?>> extends PhyloObject<C> {

	private PhyloBranch parentPhyloBranch;
	private PhyloTree tree;
	
	private String name;
	
	public PhyloBranch getParentPhyloBranch() {
		return parentPhyloBranch;
	}

	void setParentPhyloBranch(PhyloBranch parentPhyloBranch) {
		this.parentPhyloBranch = parentPhyloBranch;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setTree(PhyloTree tree) {
		this.tree = tree;
	}
	
	public PhyloTree getTree() {
		if(this.tree != null) {
			return this.tree;
		}
		return getParentPhyloBranch().getTree();
	}
	
	public abstract void accept(PhyloTreeVisitor visitor);

	@Override
	protected void copyPropertiesTo(C other) {
		super.copyPropertiesTo(other);
		other.setName(getName());
	}

}
