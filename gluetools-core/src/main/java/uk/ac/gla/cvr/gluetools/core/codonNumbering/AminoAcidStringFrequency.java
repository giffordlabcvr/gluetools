package uk.ac.gla.cvr.gluetools.core.codonNumbering;

public class AminoAcidStringFrequency {

	private String aminoAcidString;
	
	private int numMembers;
	private int totalMembers;
	private double pctMembers;
	
	public AminoAcidStringFrequency(String aminoAcidString,
			int numMembers, int totalMembers, double pctMembers) {
		super();
		this.aminoAcidString = aminoAcidString;
		this.numMembers = numMembers;
		this.totalMembers = totalMembers;
		this.pctMembers = pctMembers;
	}

	public String getAminoAcidString() {
		return aminoAcidString;
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
