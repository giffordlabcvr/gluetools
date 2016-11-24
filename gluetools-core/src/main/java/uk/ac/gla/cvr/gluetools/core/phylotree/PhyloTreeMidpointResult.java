package uk.ac.gla.cvr.gluetools.core.phylotree;


public class PhyloTreeMidpointResult {

	private PhyloBranch branch;
	private Double rootDistance;
	
	public PhyloTreeMidpointResult(PhyloBranch branch, Double rootDistance) {
		super();
		this.branch = branch;
		this.rootDistance = rootDistance;
	}
	
	public PhyloBranch getBranch() {
		return branch;
	}
	public Double getRootDistance() {
		return rootDistance;
	}
	
}
