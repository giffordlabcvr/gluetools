package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import gnu.trove.map.TIntObjectMap;

public class SamDepthCommandInterimResult extends SamBaseNucleotideCommandInterimResult {
	private TIntObjectMap<SamContributingReadsCount> relatedRefNtToInfo;

	public SamDepthCommandInterimResult(TIntObjectMap<SamContributingReadsCount> relatedRefNtToInfo) {
		super();
		this.relatedRefNtToInfo = relatedRefNtToInfo;
	}

	public TIntObjectMap<SamContributingReadsCount> getRelatedRefNtToInfo() {
		return relatedRefNtToInfo;
	}
	
	

}
