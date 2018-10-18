package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import gnu.trove.map.TIntObjectMap;

public class SamNucleotideCommandInterimResult extends SamBaseNucleotideCommandInterimResult {
	private TIntObjectMap<SamNucleotideResidueCount> relatedRefNtToInfo;

	public SamNucleotideCommandInterimResult(TIntObjectMap<SamNucleotideResidueCount> relatedRefNtToInfo) {
		super();
		this.relatedRefNtToInfo = relatedRefNtToInfo;
	}

	public TIntObjectMap<SamNucleotideResidueCount> getRelatedRefNtToInfo() {
		return relatedRefNtToInfo;
	}

}
