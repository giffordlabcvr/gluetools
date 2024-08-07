package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.util.Arrays;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.TableColumn;

public class NucleotideDeletionMatchResult extends VariationScannerMatchResult {

	// two locations within the reference region which bookend the deleted region
	private int refFirstNtDeleted;
	private int refLastNtDeleted;
	
	// two locations on the query which flank the deletion: these will always be 1 nt apart.
	private int qryLastNtBeforeDel;
	private int qryFirstNtAfterDel;

	// String of nucleotides on the reference which were deleted in the query.
	private String deletedRefNts;
	
	public NucleotideDeletionMatchResult(int refFirstNtDeleted,
			int refLastNtDeleted, int qryLastNtBeforeDel,
			int qryFirstNtAfterDel, String deletedRefNts) {
		super();
		this.refFirstNtDeleted = refFirstNtDeleted;
		this.refLastNtDeleted = refLastNtDeleted;
		this.qryLastNtBeforeDel = qryLastNtBeforeDel;
		this.qryFirstNtAfterDel = qryFirstNtAfterDel;
		this.deletedRefNts = deletedRefNts;
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

	@Override
	public int getRefStart() {
		return refFirstNtDeleted;
	}
	
	@SuppressWarnings("unchecked")
	public static List<TableColumn<NucleotideDeletionMatchResult>> getTableColumns() {
		return Arrays.asList(
				column("refFirstNtDeleted", ndmr -> ndmr.getRefFirstNtDeleted()),
				column("refLastNtDeleted", ndmr -> ndmr.getRefLastNtDeleted()),
				column("qryLastNtBeforeDel", ndmr -> ndmr.getQryLastNtBeforeDel()),
				column("qryFirstNtAfterDel", ndmr -> ndmr.getQryFirstNtAfterDel()),
				column("deletedRefNts", ndmr -> ndmr.getDeletedRefNts())
		);
	}
}
