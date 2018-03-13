package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.util.Arrays;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.TableColumn;

public class NucleotideInsertionMatchResult extends VariationScannerMatchResult {

	// two locations within the reference region which flank the inserted nucleotides 
	// these will always be 1 nt apart.
	private int refLastNtBeforeIns;
	private int refFirstNtAfterIns;
	
	// location of the insertion on the query.
	private int qryFirstInsertedNt;
	private int qryLastInsertedNt;
	
	// String of nucleotides inserted in the query.
	private String insertedQryNts;

	public NucleotideInsertionMatchResult(int refLastNtBeforeIns,
			int refFirstNtAfterIns, int qryFirstInsertedNt,
			int qryLastInsertedNt, String insertedQryNts) {
		super();
		this.refLastNtBeforeIns = refLastNtBeforeIns;
		this.refFirstNtAfterIns = refFirstNtAfterIns;
		this.qryFirstInsertedNt = qryFirstInsertedNt;
		this.qryLastInsertedNt = qryLastInsertedNt;
		this.insertedQryNts = insertedQryNts;
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

	@Override
	public int getRefStart() {
		return refLastNtBeforeIns;
	}

		
	@SuppressWarnings("unchecked")
	public static List<TableColumn<NucleotideInsertionMatchResult>> getTableColumns() {
		return Arrays.asList(
				column("refLastNtBeforeIns", nimr -> nimr.getRefLastNtBeforeIns()),
				column("refFirstNtAfterIns", nimr -> nimr.getRefFirstNtAfterIns()),
				column("qryFirstInsertedNt", nimr -> nimr.getQryFirstInsertedNt()),
				column("qryLastInsertedNt", nimr -> nimr.getQryLastInsertedNt()),
				column("insertedQryNts", nimr -> nimr.getInsertedQryNts())
		);
	}
}
