package uk.ac.gla.cvr.gluetools.core.phylotree;

public abstract class PhyloSubtree<C extends PhyloSubtree<?>> extends PhyloObject<C> {

	private PhyloBranch parentPhyloBranch;
	private PhyloTree tree;
	
	public PhyloBranch getParentPhyloBranch() {
		return parentPhyloBranch;
	}

	void setParentPhyloBranch(PhyloBranch parentPhyloBranch) {
		this.parentPhyloBranch = parentPhyloBranch;
	}

	public String getName() {
		return (String) ensureUserData().get("name");		
	}

	public void setName(String name) {
		ensureUserData().put("name", name);		
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


	
}
