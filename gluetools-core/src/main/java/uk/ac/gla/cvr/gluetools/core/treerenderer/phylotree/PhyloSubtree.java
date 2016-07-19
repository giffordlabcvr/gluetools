package uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree;

public abstract class PhyloSubtree extends PhyloObject {

	private PhyloBranch parentPhyloBranch;
	
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
	
	public abstract void accept(PhyloTreeVisitor visitor);
	
}
