package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.util.Arrays;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.TableColumn;

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
	private String insertedQryNts;

	// String of aminoAcids inserted in the query.
	private String insertedQryAas;

	// whether the insertion is codon aligned. If not then refLastCodonBeforeIns, 
	// refFirstCodonAfterIns and insertedQryAas is null.
	private boolean insertionIsCodonAligned;
	
	public AminoAcidInsertionMatchResult(String refLastCodonBeforeIns,
			String refFirstCodonAfterIns, int refLastNtBeforeIns,
			int refFirstNtAfterIns, int qryFirstInsertedNt,
			int qryLastInsertedNt, String insertedQryNts, String insertedQryAas,
			boolean insertionIsCodonAligned) {
		super();
		this.refLastCodonBeforeIns = refLastCodonBeforeIns;
		this.refFirstCodonAfterIns = refFirstCodonAfterIns;
		this.refLastNtBeforeIns = refLastNtBeforeIns;
		this.refFirstNtAfterIns = refFirstNtAfterIns;
		this.qryFirstInsertedNt = qryFirstInsertedNt;
		this.qryLastInsertedNt = qryLastInsertedNt;
		this.insertedQryNts = insertedQryNts;
		this.insertedQryAas = insertedQryAas;
		this.insertionIsCodonAligned = insertionIsCodonAligned;
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

	public String getInsertedQryNts() {
		return insertedQryNts;
	}

	public String getInsertedQryAas() {
		return insertedQryAas;
	}

	public boolean getInsertionIsCodonAligned() {
		return insertionIsCodonAligned;
	}

	@Override
	public int getRefStart() {
		return refLastNtBeforeIns;
	}
	

	@SuppressWarnings("unchecked")
	public static List<TableColumn<AminoAcidInsertionMatchResult>> getTableColumns() {
		return Arrays.asList(
				column("refLastCodonBeforeIns", aaimr -> aaimr.getRefLastCodonBeforeIns()),
				column("refFirstCodonAfterIns", aaimr -> aaimr.getRefFirstCodonAfterIns()),
				column("refLastNtBeforeIns", aaimr -> aaimr.getRefLastNtBeforeIns()),
				column("refFirstNtAfterIns", aaimr -> aaimr.getRefFirstNtAfterIns()),
				column("qryFirstInsertedNt", aaimr -> aaimr.getQryFirstInsertedNt()),
				column("qryLastInsertedNt", aaimr -> aaimr.getQryLastInsertedNt()),
				column("insertedQryNts", aaimr -> aaimr.getInsertedQryNts()),
				column("insertedQryAas", aaimr -> aaimr.getInsertedQryAas()),
				column("insertionIsCodonAligned", aaimr -> aaimr.getInsertionIsCodonAligned())
		);
	}
}
