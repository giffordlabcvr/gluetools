package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

public class FeatureCoverage {

	private String relRefName;
	private String featureName;
	private Double refCoveragePct;
	
	
	public FeatureCoverage(String relRefName, String featureName, Double refCoveragePct) {
		super();
		this.relRefName = relRefName;
		this.featureName = featureName;
		this.refCoveragePct = refCoveragePct;
	}

	public String getRelRefName() {
		return relRefName;
	}

	public String getFeatureName() {
		return featureName;
	}

	public Double getRefCoveragePct() {
		return refCoveragePct;
	}
	
}
