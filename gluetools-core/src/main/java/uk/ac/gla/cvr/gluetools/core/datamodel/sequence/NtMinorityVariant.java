package uk.ac.gla.cvr.gluetools.core.datamodel.sequence;

import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;

public class NtMinorityVariant {

	private int ntIndex;
	private char ntValue;
	private double proportion;
	
	public NtMinorityVariant(int ntIndex, char minorityNucleotide, double proportion) {
		super();
		this.ntIndex = ntIndex;
		this.ntValue = minorityNucleotide;
		this.proportion = proportion;
	}
	
	public int getNtIndex() {
		return ntIndex;
	}
	public char getNtValue() {
		return ntValue;
	}
	public double getProportion() {
		return proportion;
	}
	
	public void toDocument(ObjectBuilder minorityVariantObj) {
		minorityVariantObj.setString("ntValue", new String(new char[]{ntValue}));
		minorityVariantObj.setInt("ntIndex", ntIndex);
		minorityVariantObj.setDouble("proportion", proportion);
	}
	
}
