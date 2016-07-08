package uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree;

public abstract class PhyloSubtree {

	private String name;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public abstract void accept(PhyloTreeVisitor visitor);
	
}
