package uk.ac.gla.cvr.gluetools.core.variationscanner;

public class NucleotideInsertionMatchResult extends VariationScannerMatchResult {

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

	public NucleotideInsertionMatchResult(String refLastCodonBeforeIns,
			String refFirstCodonAfterIns, int refLastNtBeforeIns,
			int refFirstNtAfterIns, int qryFirstInsertedNt,
			int qryLastInsertedNt, String insertedRefNts, String insertedRefAas) {
		super();
		this.refLastCodonBeforeIns = refLastCodonBeforeIns;
		this.refFirstCodonAfterIns = refFirstCodonAfterIns;
		this.refLastNtBeforeIns = refLastNtBeforeIns;
		this.refFirstNtAfterIns = refFirstNtAfterIns;
		this.qryFirstInsertedNt = qryFirstInsertedNt;
		this.qryLastInsertedNt = qryLastInsertedNt;
		this.insertedRefNts = insertedRefNts;
		this.insertedRefAas = insertedRefAas;
	}

	public String getRefLastCodonBeforeIns() {
		return refLastCodonBeforeIns;
	}

	public String getRefFirstCodonAfterIns() {
		return refFirstCodonAfterIns;
	}

	public int getRefLastNtBeforeIns() {
		return refLastNtBeforeIns;
	}

	public int getRefFirstNtAfterIns() {
		return refFirstNtAfterIns;
	}

	public int getQryFirstInsertedNt() {
		return qryFirstInsertedNt;
	}

	public int getQryLastInsertedNt() {
		return qryLastInsertedNt;
	}

	public String getInsertedRefNts() {
		return insertedRefNts;
	}

	public String getInsertedRefAas() {
		return insertedRefAas;
	}

	@Override
	public int getRefStart() {
		return refLastNtBeforeIns;
	}


}
