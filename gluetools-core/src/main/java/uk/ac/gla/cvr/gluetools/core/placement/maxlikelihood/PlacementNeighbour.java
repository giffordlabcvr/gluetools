package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import java.math.BigDecimal;

import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;

public class PlacementNeighbour {

	private PhyloLeaf phyloLeaf;
	private BigDecimal distance;
	
	public PlacementNeighbour(PhyloLeaf phyloLeaf, BigDecimal distance) {
		super();
		this.phyloLeaf = phyloLeaf;
		this.distance = distance;
	}

	public PhyloLeaf getPhyloLeaf() {
		return phyloLeaf;
	}

	public BigDecimal getDistance() {
		return distance;
	}

	
	
}
