package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.util.Arrays;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.TableColumn;

public class NucleotideRegexPolymorphismMatchResult extends VariationScannerMatchResult {

	// reference NT locations bookending the match
	private int refNtStart;
	private int refNtEnd;

	// query NT locations bookending the match
	private int queryNtStart;
	private int queryNtEnd;

	// nucleotides underlying the matching AAs
	private String queryNts;

	@Override
	public int getRefStart() {
		return refNtStart;
	}

	public NucleotideRegexPolymorphismMatchResult(int refNtStart, int refNtEnd,
			int queryNtStart, int queryNtEnd, String queryNts) {
		super();
		this.refNtStart = refNtStart;
		this.refNtEnd = refNtEnd;
		this.queryNtStart = queryNtStart;
		this.queryNtEnd = queryNtEnd;
		this.queryNts = queryNts;
	}

	public int getRefNtStart() {
		return refNtStart;
	}

	public int getRefNtEnd() {
		return refNtEnd;
	}

	public int getQueryNtStart() {
		return queryNtStart;
	}

	public int getQueryNtEnd() {
		return queryNtEnd;
	}

	public String getQueryNts() {
		return queryNts;
	}
		
	@SuppressWarnings("unchecked")
	public static List<TableColumn<NucleotideRegexPolymorphismMatchResult>> getTableColumns() {
		return Arrays.asList(
				column("refNtStart", npmr -> npmr.getRefNtStart()),
				column("refNtEnd", npmr -> npmr.getRefNtEnd()),
				column("queryNtStart", npmr -> npmr.getQueryNtStart()),
				column("queryNtEnd", npmr -> npmr.getQueryNtEnd()),
				column("queryNts", npmr -> npmr.getQueryNts())
		);
	}
	
}
