package uk.ac.gla.cvr.gluetools.core.segments;

/**
 * Segment containing amino acids. Note that reference coordinates in this case relate to codons.
 *
 */
public interface IAaReferenceSegment extends IReferenceSegment {

	public CharSequence getAminoAcids();
	
}
