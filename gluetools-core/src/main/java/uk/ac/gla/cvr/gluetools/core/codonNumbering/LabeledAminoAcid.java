package uk.ac.gla.cvr.gluetools.core.codonNumbering;

public class LabeledAminoAcid {

	private LabeledCodon labeledCodon;
	private String aminoAcid;
	
	public LabeledAminoAcid(LabeledCodon labeledCodon, String aminoAcid) {
		this.labeledCodon = labeledCodon;
		this.aminoAcid = aminoAcid;
	}

	public LabeledCodon getLabeledCodon() {
		return labeledCodon;
	}

	public String getAminoAcid() {
		return aminoAcid;
	}
}
