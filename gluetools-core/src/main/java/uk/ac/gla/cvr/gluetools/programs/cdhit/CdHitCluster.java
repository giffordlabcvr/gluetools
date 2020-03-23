package uk.ac.gla.cvr.gluetools.programs.cdhit;

import java.util.List;

public class CdHitCluster {

	private Integer clusterNumber;
	private String representativeSeqId;
	private List<String> otherSeqIds;
	
	public CdHitCluster(Integer clusterNumber, String representativeSeqId, List<String> otherSeqIds) {
		super();
		this.clusterNumber = clusterNumber;
		this.representativeSeqId = representativeSeqId;
		this.otherSeqIds = otherSeqIds;
	}

	public Integer getClusterNumber() {
		return clusterNumber;
	}

	public String getRepresentativeSeqId() {
		return representativeSeqId;
	}

	public List<String> getOtherSeqIds() {
		return otherSeqIds;
	}
	
	
}
