package uk.ac.gla.cvr.gluetools.core.segments;

/**
 * Segment containing amino acids. Note that reference coordinates in this case relate to codons.
 *
 */
public interface IAaReferenceSegment extends IReferenceSegment {

	public static final String AMINO_ACIDS = "aminoAcids";

	public CharSequence getAminoAcids();

	public default CharSequence getAminoAcidsSubsequence(int startIndex, int endIndex) {
		return getAminoAcids().subSequence((startIndex-getRefStart()), (endIndex-getRefStart()) + 1);
	}

	
}
