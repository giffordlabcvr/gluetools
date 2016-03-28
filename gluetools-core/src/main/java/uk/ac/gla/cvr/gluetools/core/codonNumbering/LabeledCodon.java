package uk.ac.gla.cvr.gluetools.core.codonNumbering;

public class LabeledCodon {

	private String codonLabel;
	private int ntStart;
	
	
	public LabeledCodon(String codonLabel, int ntStart) {
		super();
		this.codonLabel = codonLabel;
		this.ntStart = ntStart;
	}

	public String getCodonLabel() {
		return codonLabel;
	}

	public int getNtStart() {
		return ntStart;
	}
	
}
