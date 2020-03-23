package uk.ac.gla.cvr.gluetools.programs.cdhit;

import java.util.Map;

public class CdHitEstGenerateClustersAlignmentResultRow {

	private Map<String, String> memberPkMap;
	private int clusterNumber;
	private boolean isRepresentative;
	
	public CdHitEstGenerateClustersAlignmentResultRow(Map<String, String> memberPkMap, int clusterNumber, boolean isRepresentative) {
		super();
		this.memberPkMap = memberPkMap;
		this.clusterNumber = clusterNumber;
		this.isRepresentative = isRepresentative;
	}

	public Map<String, String> getMemberPkMap() {
		return memberPkMap;
	}

	public int getClusterNumber() {
		return clusterNumber;
	}

	public boolean isRepresentative() {
		return isRepresentative;
	}
	
	
	
}
