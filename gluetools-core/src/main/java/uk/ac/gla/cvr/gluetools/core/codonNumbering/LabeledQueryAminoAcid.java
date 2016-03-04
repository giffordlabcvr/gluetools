package uk.ac.gla.cvr.gluetools.core.codonNumbering;

public class LabeledQueryAminoAcid {

	private LabeledAminoAcid labeledAminoAcid;
	private int queryNt;
	
	public LabeledQueryAminoAcid(LabeledAminoAcid labeledAminoAcid, int queryNt) {
		this.labeledAminoAcid = labeledAminoAcid;
		this.queryNt = queryNt;
	}

	public LabeledAminoAcid getLabeledAminoAcid() {
		return labeledAminoAcid;
	}

	public int getQueryNt() {
		return queryNt;
	}
}
