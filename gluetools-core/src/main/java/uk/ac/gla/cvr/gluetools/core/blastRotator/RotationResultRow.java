package uk.ac.gla.cvr.gluetools.core.blastRotator;

public class RotationResultRow {

	public enum Status {
		NO_BLAST_HITS,
		MULTIPLE_BLAST_HITS,
		NO_ACCEPTABLE_HSPS,
		NO_ROTATION_NECESSARY,
		OVERLAPPING_QUERY_HSPS,
		OVERLAPPING_HIT_HSPS,
		ROTATION_NECESSARY
	}
	
	private String querySequenceId;
	private Status status;
	private Integer rotationNts;
	
	public RotationResultRow(String querySequenceId, Status status, Integer rotationNts) {
		super();
		this.querySequenceId = querySequenceId;
		this.status = status;
		this.rotationNts = rotationNts;
	}
	
	public String getQuerySequenceId() {
		return querySequenceId;
	}

	public Status getStatus() {
		return status;
	}

	public Integer getRotationNts() {
		return rotationNts;
	}
	
}