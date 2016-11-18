package uk.ac.gla.cvr.gluetools.core.phylotree;

import java.util.ArrayList;
import java.util.List;

public class PhyloLeafLister implements PhyloTreeVisitor {

	private List<PhyloLeaf> phyloLeaves = new ArrayList<PhyloLeaf>();

	public List<PhyloLeaf> getPhyloLeaves() {
		return phyloLeaves;
	}

	@Override
	public void visitLeaf(PhyloLeaf phyloLeaf) {
		phyloLeaves.add(phyloLeaf);
	}
	
}
