package uk.ac.gla.cvr.gluetools.core.genotyping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

public class GenotypingResult {


	// FASTA ID of the sequence (for genotyping of file query sequences)
	// For genotyping of stored query sequences, sequenceName will be <sourceName>/<sequenceID>
	private String sequenceName;
	
	// Alignment / source / seq. ID of the alignment member which genotyping believes is the closest 
	// to the query.
	// Mandatory
	private String closestMemberAlmtName;
	private String closestMemberSourceName;
	private String closestMemberSequenceID;

	// Name of the (clade) which genotyping grouped the query inside
	// Optional
	// May or may not be equal to the alignment of the closest member (it may be an ancestor alignment).
	private String groupingAlignmentName;
	

	
	// Boolean, what percentage of the closest member does the query cover
	// Mandatory
	private String queryClosestMemberCoverage;

	// Double, of the area covered, what percentage of nuclotides are identical to those of the nearest reference.
	// Mandatory
	private String queryClosestMemberNtIdentity;

	// Pairwise alignment between the query and the closest member.
	// Mandatory
	private List<QueryAlignedSegment> queryAlignedSegments;
	
	// additional data keys
	private Map<String, Object> additionalData = new LinkedHashMap<String, Object>();
	

	public Map<String, Object> getAdditionalData() {
		return additionalData;
	}

	public void setAdditionalData(Map<String, Object> additionalData) {
		this.additionalData = additionalData;
	}

	public List<QueryAlignedSegment> getQueryAlignedSegments() {
		return queryAlignedSegments;
	}

	public void setQueryAlignedSegments(
			List<QueryAlignedSegment> queryAlignedSegments) {
		this.queryAlignedSegments = queryAlignedSegments;
	}

	public String getSequenceName() {
		return sequenceName;
	}

	public void setSequenceName(String sequenceName) {
		this.sequenceName = sequenceName;
	}

	public String getClosestMemberAlmtName() {
		return closestMemberAlmtName;
	}

	public void setClosestMemberAlmtName(String closestMemberAlmtName) {
		this.closestMemberAlmtName = closestMemberAlmtName;
	}

	public String getClosestMemberSourceName() {
		return closestMemberSourceName;
	}

	public void setClosestMemberSourceName(String closestMemberSourceName) {
		this.closestMemberSourceName = closestMemberSourceName;
	}

	public String getClosestMemberSequenceID() {
		return closestMemberSequenceID;
	}

	public void setClosestMemberSequenceID(String closestMemberSequenceID) {
		this.closestMemberSequenceID = closestMemberSequenceID;
	}

	public String getQueryClosestMemberCoverage() {
		return queryClosestMemberCoverage;
	}

	public void setQueryClosestMemberCoverage(String queryClosestMemberCoverage) {
		this.queryClosestMemberCoverage = queryClosestMemberCoverage;
	}

	
	public String getQueryClosestMemberNtIdentity() {
		return queryClosestMemberNtIdentity;
	}

	public void setQueryClosestMemberNtIdentity(String queryClosestMemberNtIdentity) {
		this.queryClosestMemberNtIdentity = queryClosestMemberNtIdentity;
	}

	public String getGroupingAlignmentName() {
		return groupingAlignmentName;
	}

	public void setGroupingAlignmentName(String groupingAlignmentName) {
		this.groupingAlignmentName = groupingAlignmentName;
	}

	
}
