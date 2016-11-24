package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import java.util.Map;

public class PlacementResult {


	// FASTA ID of the sequence (for placement of file query sequences)
	// For placement of stored query sequences, sequenceName will be <sourceName>/<sequenceID>
	private String sequenceName;
	
	// Alignment / source / seq. ID of the alignment member which placement believes is the closest 
	// to the query.
	// Mandatory
	private Map<String,String> closestMemberPkMap;

	// Name of the (clade) which placement grouped the query inside
	// Optional
	// May or may not be equal to the alignment of the closest member (it may be an ancestor alignment).
	private String groupingAlignmentName;
	
	private Double distanceToClosestMember;
	
	private double likeWeightRatio;
	
	public String getSequenceName() {
		return sequenceName;
	}

	public void setSequenceName(String sequenceName) {
		this.sequenceName = sequenceName;
	}
	
	public Map<String, String> getClosestMemberPkMap() {
		return closestMemberPkMap;
	}

	public void setClosestMemberPkMap(Map<String, String> closestMemberPkMap) {
		this.closestMemberPkMap = closestMemberPkMap;
	}

	public String getGroupingAlignmentName() {
		return groupingAlignmentName;
	}

	public void setGroupingAlignmentName(String groupingAlignmentName) {
		this.groupingAlignmentName = groupingAlignmentName;
	}

	public Double getDistanceToClosestMember() {
		return distanceToClosestMember;
	}

	public void setDistanceToClosestMember(Double distanceToClosestMember) {
		this.distanceToClosestMember = distanceToClosestMember;
	}

	public double getLikeWeightRatio() {
		return likeWeightRatio;
	}

	public void setLikeWeightRatio(double likeWeightRatio) {
		this.likeWeightRatio = likeWeightRatio;
	}
	
}
