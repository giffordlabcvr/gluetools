package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

public class VariationScanReadCount {

	private String variationName;
	private int readsWherePresent;
	private double pctWherePresent;
	private int readsWhereAbsent;
	private double pctWhereAbsent;
	
	public VariationScanReadCount(String variationName, int readsWherePresent,
			double pctWherePresent, int readsWhereAbsent, double pctWhereAbsent) {
		super();
		this.variationName = variationName;
		this.readsWherePresent = readsWherePresent;
		this.pctWherePresent = pctWherePresent;
		this.readsWhereAbsent = readsWhereAbsent;
		this.pctWhereAbsent = pctWhereAbsent;
	}
	
	public String getVariationName() {
		return variationName;
	}
	public int getReadsWherePresent() {
		return readsWherePresent;
	}
	public double getPctWherePresent() {
		return pctWherePresent;
	}
	public int getReadsWhereAbsent() {
		return readsWhereAbsent;
	}
	public double getPctWhereAbsent() {
		return pctWhereAbsent;
	}
	
	
	
}
