package uk.ac.gla.cvr.gluetools.core.segments;

public interface INtReferenceSegment extends IReferenceSegment {

	public CharSequence getNucleotides();
	
	public char ntAtRefLocation(int refLocation);
	
}
