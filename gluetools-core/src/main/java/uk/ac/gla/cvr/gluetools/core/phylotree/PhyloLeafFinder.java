package uk.ac.gla.cvr.gluetools.core.phylotree;

import java.util.function.Predicate;

public class PhyloLeafFinder implements PhyloTreeVisitor {

	private PhyloLeaf phyloLeaf;
	private Predicate<PhyloLeaf> predicate;
	
	public PhyloLeafFinder(Predicate<PhyloLeaf> predicate) {
		super();
		this.predicate = predicate;
	}

	public PhyloLeaf getPhyloLeaf() {
		return phyloLeaf;
	}

	@Override
	public void visitLeaf(PhyloLeaf phyloLeaf) {
		if(this.phyloLeaf == null && predicate.test(phyloLeaf)) {
			this.phyloLeaf = phyloLeaf;
		}
	}
	
}
