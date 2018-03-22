package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.util.Arrays;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.TableColumn;

public class NucleotideSimplePolymorphismMatchResult extends VariationScannerMatchResult {

	// reference NT locations bookending the match
	private int refNtStart;
	private int refNtEnd;

	// query NT locations bookending the match
	private int queryNtStart;
	private int queryNtEnd;

	// nucleotides underlying the matching AAs
	private String queryNts;

	// product of NT probabilities (<1 where there were ambiguous NTs).
	private double combinedNtFraction;
	
	@Override
	public int getRefStart() {
		return refNtStart;
	}

	public NucleotideSimplePolymorphismMatchResult(int refNtStart, int refNtEnd,
			int queryNtStart, int queryNtEnd, String queryNts, double combinedNtFraction) {
		super();
		this.refNtStart = refNtStart;
		this.refNtEnd = refNtEnd;
		this.queryNtStart = queryNtStart;
		this.queryNtEnd = queryNtEnd;
		this.queryNts = queryNts;
		this.combinedNtFraction = combinedNtFraction;
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
		
	public double getCombinedNtFraction() {
		return combinedNtFraction;
	}

	@SuppressWarnings("unchecked")
	public static List<TableColumn<NucleotideSimplePolymorphismMatchResult>> getTableColumns() {
		return Arrays.asList(
				column("refNtStart", npmr -> npmr.getRefNtStart()),
				column("refNtEnd", npmr -> npmr.getRefNtEnd()),
				column("queryNtStart", npmr -> npmr.getQueryNtStart()),
				column("queryNtEnd", npmr -> npmr.getQueryNtEnd()),
				column("queryNts", npmr -> npmr.getQueryNts()),
				column("combinedNtFraction", npmr -> npmr.getCombinedNtFraction())
		);
	}
	
}
