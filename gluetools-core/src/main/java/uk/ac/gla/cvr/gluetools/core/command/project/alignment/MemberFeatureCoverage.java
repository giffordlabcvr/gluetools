package uk.ac.gla.cvr.gluetools.core.command.project.alignment;

import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;

public class MemberFeatureCoverage {

	private AlignmentMember alignmentMember;
	private Double featureReferenceNtCoverage;
	
	public MemberFeatureCoverage(AlignmentMember alignmentMember, Double featureReferenceNtCoverage) {
		super();
		this.alignmentMember = alignmentMember;
		this.featureReferenceNtCoverage = featureReferenceNtCoverage;
	}

	public AlignmentMember getAlignmentMember() {
		return alignmentMember;
	}

	public Double getFeatureReferenceNtCoverage() {
		return featureReferenceNtCoverage;
	}
	
}
