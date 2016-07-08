package uk.ac.gla.cvr.gluetools.core.treerenderer.phylotree;

public class PhyloLeaf extends PhyloSubtree {

	@Override
	public void accept(PhyloTreeVisitor visitor) {
		visitor.visitLeaf(this);
	}

}
