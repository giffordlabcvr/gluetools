package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.util.Arrays;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.TableColumn;

public class AminoAcidDeletionMatchResult extends VariationScannerMatchResult {

	// two labeled codons within the reference region which bookend the deleted region
	private String refFirstCodonDeleted;
	private String refLastCodonDeleted;
	
	// two locations within the reference region which bookend the deleted region
	private int refFirstNtDeleted;
	private int refLastNtDeleted;
	
	// two locations on the query which flank the deletion: these will always be 1 nt apart.
	private int qryLastNtBeforeDel;
	private int qryFirstNtAfterDel;

	// String of nucleotides on the reference which were deleted in the query.
	private String deletedRefNts;

	// String of aminoAcids on the reference which were deleted in the query.
	private String deletedRefAas;

	public AminoAcidDeletionMatchResult(String refFirstCodonDeleted,
			String refLastCodonDeleted, int refFirstNtDeleted,
			int refLastNtDeleted, int qryLastNtBeforeDel,
			int qryFirstNtAfterDel, String deletedRefNts, String deletedRefAas) {
		super();
		this.refFirstCodonDeleted = refFirstCodonDeleted;
		this.refLastCodonDeleted = refLastCodonDeleted;
		this.refFirstNtDeleted = refFirstNtDeleted;
		this.refLastNtDeleted = refLastNtDeleted;
		this.qryLastNtBeforeDel = qryLastNtBeforeDel;
		this.qryFirstNtAfterDel = qryFirstNtAfterDel;
		this.deletedRefNts = deletedRefNts;
		this.deletedRefAas = deletedRefAas;
	}

	public String getRefFirstCodonDeleted() {
		return refFirstCodonDeleted;
	}

	public String getRefLastCodonDeleted() {
		return refLastCodonDeleted;
	}

	public int getRefFirstNtDeleted() {
		return refFirstNtDeleted;
	}

	public int getRefLastNtDeleted() {
		return refLastNtDeleted;
	}

	public int getQryLastNtBeforeDel() {
		return qryLastNtBeforeDel;
	}

	public int getQryFirstNtAfterDel() {
		return qryFirstNtAfterDel;
	}

	public String getDeletedRefNts() {
		return deletedRefNts;
	}

	public String getDeletedRefAas() {
		return deletedRefAas;
	}

	@Override
	public int getRefStart() {
		return refFirstNtDeleted;
	}

	@SuppressWarnings("unchecked")
	public static List<TableColumn<AminoAcidDeletionMatchResult>> getTableColumns() {
		return Arrays.asList(
				column("refFirstCodonDeleted", aadmr -> aadmr.getRefFirstCodonDeleted()),
				column("refLastCodonDeleted", aadmr -> aadmr.getRefLastCodonDeleted()),
				column("refFirstNtDeleted", aadmr -> aadmr.getRefFirstNtDeleted()),
				column("refLastNtDeleted", aadmr -> aadmr.getRefLastNtDeleted()),
				column("qryLastNtBeforeDel", aadmr -> aadmr.getQryLastNtBeforeDel()),
				column("qryFirstNtAfterDel", aadmr -> aadmr.getQryFirstNtAfterDel()),
				column("deletedRefNts", aadmr -> aadmr.getDeletedRefNts()),
				column("deletedRefAas", aadmr -> aadmr.getDeletedRefAas())
		);
	}
}
