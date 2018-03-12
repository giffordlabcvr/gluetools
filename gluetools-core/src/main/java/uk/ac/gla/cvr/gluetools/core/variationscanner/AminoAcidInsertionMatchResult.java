package uk.ac.gla.cvr.gluetools.core.variationscanner;

public class AminoAcidInsertionMatchResult extends VariationScannerMatchResult {

	// two labeled codons within the reference region which flank the inserted residues
	// these will be adjacent on the reference.
	private String refLastCodonBeforeIns;
	private String refFirstCodonAfterIns;
	
	// two locations within the reference region which flank the inserted nucleotides 
	// these will always be 1 nt apart.
	private int refLastNtBeforeIns;
	private int refFirstNtAfterIns;
	
	// location of the insertion on the query.
	private int qryFirstInsertedNt;
	private int qryLastInsertedNt;
	
	// String of nucleotides inserted in the query.
	private String insertedRefNts;

	// String of aminoAcids inserted in the query.
	private String insertedRefAas;

	@Override
	public int getRefStart() {
		return refLastNtBeforeIns;
	}

	
}
