package uk.ac.gla.cvr.gluetools.core.codonNumbering;

public class LabeledAminoAcidFrequency {

	private LabeledAminoAcid labeledAminoAcid;
	
	private int numMembers;
	private int totalMembers;
	private double pctMembers;
	
	public LabeledAminoAcidFrequency(LabeledAminoAcid labeledAminoAcid,
			int numMembers, int totalMembers, double pctMembers) {
		super();
		this.labeledAminoAcid = labeledAminoAcid;
		this.numMembers = numMembers;
		this.totalMembers = totalMembers;
		this.pctMembers = pctMembers;
	}

	public LabeledAminoAcid getLabeledAminoAcid() {
		return labeledAminoAcid;
	}

	public int getNumMembers() {
		return numMembers;
	}

	public int getTotalMembers() {
		return totalMembers;
	}

	public double getPctMembers() {
		return pctMembers;
	}
	
}
