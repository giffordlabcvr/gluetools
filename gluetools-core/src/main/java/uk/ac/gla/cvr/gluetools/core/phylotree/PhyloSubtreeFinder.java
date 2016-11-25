package uk.ac.gla.cvr.gluetools.core.phylotree;

import java.util.function.Predicate;

public class PhyloSubtreeFinder implements PhyloTreeVisitor {

	private PhyloSubtree<?> phyloSubtree;
	private Predicate<PhyloSubtree<?>> predicate;
	
	public PhyloSubtreeFinder(Predicate<PhyloSubtree<?>> predicate) {
		super();
		this.predicate = predicate;
	}

	public PhyloSubtree<?> getPhyloSubtree() {
		return phyloSubtree;
	}

	@Override
	public void visitLeaf(PhyloLeaf phyloLeaf) {
		if(this.phyloSubtree == null && predicate.test(phyloLeaf)) {
			this.phyloSubtree = phyloLeaf;
		}
	}

	@Override
	public void preVisitInternal(PhyloInternal phyloInternal) {
		if(this.phyloSubtree == null && predicate.test(phyloInternal)) {
			this.phyloSubtree = phyloInternal;
		}
	}

}
