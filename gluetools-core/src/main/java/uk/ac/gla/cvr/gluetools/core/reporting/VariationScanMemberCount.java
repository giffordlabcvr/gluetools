package uk.ac.gla.cvr.gluetools.core.reporting;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;

public class VariationScanMemberCount {

	private Variation variation;
	private int membersWherePresent;
	private double pctWherePresent;
	private int membersWhereAbsent;
	private double pctWhereAbsent;
	
	public VariationScanMemberCount(Variation variation, int membersWherePresent,
			double pctWherePresent, int membersWhereAbsent, double pctWhereAbsent) {
		super();
		this.variation = variation;
		this.membersWherePresent = membersWherePresent;
		this.pctWherePresent = pctWherePresent;
		this.membersWhereAbsent = membersWhereAbsent;
		this.pctWhereAbsent = pctWhereAbsent;
	}
	
	public Variation getVariation() {
		return variation;
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
	
	public static void sortVariationScanMemberCounts(List<VariationScanMemberCount> variationScanMemberCounts) {
		Comparator<VariationScanMemberCount> comparator = new Comparator<VariationScanMemberCount>(){
			@Override
			public int compare(VariationScanMemberCount o1, VariationScanMemberCount o2) {
				int comp = o1.getVariation().getFeatureLoc().getReferenceSequence().getName().compareTo(o2.getVariation().getFeatureLoc().getReferenceSequence().getName());
				Integer minLocStart1 = o1.getVariation().minLocStart();
				Integer minLocStart2 = o2.getVariation().minLocStart();
				if(comp == 0 && minLocStart1 != null && minLocStart2 != null) {
					comp = Integer.compare(minLocStart1, minLocStart2);
				}
				if(comp == 0) {
					comp = o1.getVariation().getName().compareTo(o2.getVariation().getName());
				}
				return comp;
			}
		};
		Collections.sort(variationScanMemberCounts, comparator);
	}

	
}
