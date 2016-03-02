package uk.ac.gla.cvr.gluetools.core.codonNumbering;

public class LabeledCodon {

	private String label;
	private int ntStart;
	
	
	public LabeledCodon(String label, int ntStart) {
		super();
		this.label = label;
		this.ntStart = ntStart;
	}


	public String getLabel() {
		return label;
	}


	public int getNtStart() {
		return ntStart;
	}
	
	
	
}
