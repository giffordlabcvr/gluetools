package uk.ac.gla.cvr.gluetools.core.segments;

public interface INtReferenceSegment extends IReferenceSegment {

	public static final String NUCLEOTIDES = "nucleotides";

	public CharSequence getNucleotides();
	
	public default char ntAtRefLocation(int refLocation) {
		return getNucleotides().charAt(refLocation - getRefStart());
	}

	public int ntIndexAtRefLoction(int refLocation);
	
}
