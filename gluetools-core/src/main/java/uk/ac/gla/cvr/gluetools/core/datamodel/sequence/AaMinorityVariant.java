package uk.ac.gla.cvr.gluetools.core.datamodel.sequence;

import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;

public class AaMinorityVariant {

	private int aaIndex;
	private char aaValue;
	private double proportion;
	
	public AaMinorityVariant(int aaIndex, char minorityAminoAcid, double proportion) {
		super();
		this.aaIndex = aaIndex;
		this.aaValue = minorityAminoAcid;
		this.proportion = proportion;
	}
	
	public int getAaIndex() {
		return aaIndex;
	}
	public char getAaValue() {
		return aaValue;
	}
	public double getProportion() {
		return proportion;
	}
	
	public void toDocument(ObjectBuilder minorityVariantObj) {
		minorityVariantObj.setString("aaValue", new String(new char[]{aaValue}));
		minorityVariantObj.setInt("aaIndex", aaIndex);
		minorityVariantObj.setDouble("proportion", proportion);
	}
	
	public AaMinorityVariant clone() {
		return new AaMinorityVariant(getAaIndex(), getAaValue(), getProportion());
	}
	
	public void translate(int offset) {
		aaIndex += offset;
	}
	
}
