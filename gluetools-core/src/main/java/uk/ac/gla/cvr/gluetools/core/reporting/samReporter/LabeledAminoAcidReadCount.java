package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledAminoAcid;

public class LabeledAminoAcidReadCount {
	
	private LabeledAminoAcid labeledAminoAcid;
	private int samRefNt;
	private int readsWithAminoAcid;
	private double percentReadsWithAminoAcid;
	
	public LabeledAminoAcidReadCount(LabeledAminoAcid labeledAminoAcid,
			int samRefNt, int readsWithAminoAcid, double percentReadsWithAminoAcid) {
		super();
		this.labeledAminoAcid = labeledAminoAcid;
		this.samRefNt = samRefNt;
		this.readsWithAminoAcid = readsWithAminoAcid;
		this.percentReadsWithAminoAcid = percentReadsWithAminoAcid;
	}

	public LabeledAminoAcid getLabeledAminoAcid() {
		return labeledAminoAcid;
	}

	public int getReadsWithAminoAcid() {
		return readsWithAminoAcid;
	}

	public double getPercentReadsWithAminoAcid() {
		return percentReadsWithAminoAcid;
	}

	public int getSamRefNt() {
		return samRefNt;
	}
	
	
	
}