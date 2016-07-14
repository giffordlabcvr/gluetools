package uk.ac.gla.cvr.gluetools.core.genotyping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

public class GenotypingResult {


	// FASTA ID of the sequence (for genotyping of file query sequences)
	// For genotyping of stored query sequences, sequenceName will be <sourceName>/<sequenceID>
	private String sequenceName;
	
	// Name of the most recent alignment (clade) which genotyping grouped the query inside
	// Mandatory
	private String alignmentName;
	
	// String, name of the reference sequence which genotyping believes is the closest reference sequence to the query.
	// Mandatory
	private String closestReference;

	// Boolean, what percentage of the closest reference does the query cover
	// Mandatory
	private String queryReferenceCoverage;

	// Double, of the area covered, what percentage of nuclotides are identical to those of the nearest reference.
	// Mandatory
	private String referenceNtIdentity;

	// Mandatory
	private List<QueryAlignedSegment> queryAlignedSegments;
	
	// additional data keys
	private Map<String, Object> additionalData = new LinkedHashMap<String, Object>();
	

	public Map<String, Object> getAdditionalData() {
		return additionalData;
	}

	public String getAlignmentName() {
		return alignmentName;
	}

	public void setAdditionalData(Map<String, Object> additionalData) {
		this.additionalData = additionalData;
	}

	public void setAlignmentName(String alignmentName) {
		this.alignmentName = alignmentName;
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

	public String getClosestReference() {
		return closestReference;
	}

	public void setClosestReference(String closestReference) {
		this.closestReference = closestReference;
	}

	public String getQueryReferenceCoverage() {
		return queryReferenceCoverage;
	}

	public void setQueryReferenceCoverage(String queryReferenceCoverage) {
		this.queryReferenceCoverage = queryReferenceCoverage;
	}

	public String getReferenceNtIdentity() {
		return referenceNtIdentity;
	}

	public void setReferenceNtIdentity(String referenceNtIdentity) {
		this.referenceNtIdentity = referenceNtIdentity;
	}
	
	
}
