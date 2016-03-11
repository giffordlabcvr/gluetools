package uk.ac.gla.cvr.gluetools.core.reporting;

public class VariationScanMemberCount {

	private String variationName;
	private int membersWherePresent;
	private double pctWherePresent;
	private int membersWhereAbsent;
	private double pctWhereAbsent;
	
	public VariationScanMemberCount(String variationName, int membersWherePresent,
			double pctWherePresent, int membersWhereAbsent, double pctWhereAbsent) {
		super();
		this.variationName = variationName;
		this.membersWherePresent = membersWherePresent;
		this.pctWherePresent = pctWherePresent;
		this.membersWhereAbsent = membersWhereAbsent;
		this.pctWhereAbsent = pctWhereAbsent;
	}
	
	public String getVariationName() {
		return variationName;
	}
	public int getMembersWherePresent() {
		return membersWherePresent;
	}
	public double getPctWherePresent() {
		return pctWherePresent;
	}
	public int getMembersWhereAbsent() {
		return membersWhereAbsent;
	}
	public double getPctWhereAbsent() {
		return pctWhereAbsent;
	}
	
	
	
}
