package uk.ac.gla.cvr.gluetools.core.phylotree;

import java.math.BigDecimal;

public class PhyloTreeMidpointResult {

	private PhyloBranch branch;
	private BigDecimal rootDistance;
	
	public PhyloTreeMidpointResult(PhyloBranch branch, BigDecimal rootDistance) {
		super();
		this.branch = branch;
		this.rootDistance = rootDistance;
	}
	
	public PhyloBranch getBranch() {
		return branch;
	}
	public BigDecimal getRootDistance() {
		return rootDistance;
	}
	
}
