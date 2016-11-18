package uk.ac.gla.cvr.gluetools.core.phylotree;

public class PhyloLeaf extends PhyloSubtree<PhyloLeaf> {

	@Override
	public void accept(PhyloTreeVisitor visitor) {
		visitor.visitLeaf(this);
	}

	@Override
	public PhyloLeaf clone() {
		PhyloLeaf phyloLeaf = new PhyloLeaf();
		copyPropertiesTo(phyloLeaf);
		return phyloLeaf;
	}

}
