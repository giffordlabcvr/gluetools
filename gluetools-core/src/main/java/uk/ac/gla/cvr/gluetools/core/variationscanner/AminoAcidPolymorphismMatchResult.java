package uk.ac.gla.cvr.gluetools.core.variationscanner;

public class AminoAcidPolymorphismMatchResult extends VariationScannerMatchResult {

	// labeled codons bookending the match
	private String firstRefCodon;
	private String lastRefCodon;

	// reference NT locations bookending the match
	private int refNtStart;
	private int refNtEnd;

	// query NT locations bookending the match
	private int queryNtStart;
	private int queryNtEnd;

	// matching AAs in the query
	private String queryAAs;

	// nucleotides underlying the matching AAs
	private String queryNts;

	public AminoAcidPolymorphismMatchResult(String firstRefCodon,
			String lastRefCodon, int refNtStart, int refNtEnd,
			int queryNtStart, int queryNtEnd, String queryAAs, String queryNts) {
		super();
		this.firstRefCodon = firstRefCodon;
		this.lastRefCodon = lastRefCodon;
		this.refNtStart = refNtStart;
		this.refNtEnd = refNtEnd;
		this.queryNtStart = queryNtStart;
		this.queryNtEnd = queryNtEnd;
		this.queryAAs = queryAAs;
		this.queryNts = queryNts;
	}

	public String getFirstRefCodon() {
		return firstRefCodon;
	}

	public String getLastRefCodon() {
		return lastRefCodon;
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

	public String getQueryAAs() {
		return queryAAs;
	}

	public String getQueryNts() {
		return queryNts;
	}

	@Override
	public int getRefStart() {
		return refNtStart;
	}
	
}
