package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;

public class AlignmentCoverageScore {

	private AlignmentMember almtMember;
	private Double score;

	public AlignmentCoverageScore(AlignmentMember almtMember, Double score) {
		super();
		this.almtMember = almtMember;
		this.score = score;
	}
	public AlignmentMember getAlmtMember() {
		return almtMember;
	}
	public Double getScore() {
		return score;
	}
	
}
