package uk.ac.gla.cvr.gluetools.core.reporting;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;

public class VariationScanMemberCount {

	private Map<String,String> variationPkMap;
	private int minLocStart;
	private int membersWherePresent;
	private double pctWherePresent;
	private int membersWhereAbsent;
	private double pctWhereAbsent;
	
	public VariationScanMemberCount(Map<String,String> variationPkMap, int minLocStart, int membersWherePresent,
			double pctWherePresent, int membersWhereAbsent, double pctWhereAbsent) {
		super();
		this.variationPkMap = variationPkMap;
		this.membersWherePresent = membersWherePresent;
		this.pctWherePresent = pctWherePresent;
		this.membersWhereAbsent = membersWhereAbsent;
		this.pctWhereAbsent = pctWhereAbsent;
		this.minLocStart = minLocStart;
	}
	
	public Map<String, String> getVariationPkMap() {
		return variationPkMap;
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
	
	public int getMinLocStart() {
		return minLocStart;
	}

	public static void sortVariationScanMemberCounts(List<VariationScanMemberCount> variationScanMemberCounts) {
		Comparator<VariationScanMemberCount> comparator = new Comparator<VariationScanMemberCount>(){
			@Override
			public int compare(VariationScanMemberCount o1, VariationScanMemberCount o2) {
				int comp = o1.getVariationPkMap().get(Variation.REF_SEQ_NAME_PATH).compareTo(o2.getVariationPkMap().get(Variation.REF_SEQ_NAME_PATH));
				Integer minLocStart1 = o1.getMinLocStart();
				Integer minLocStart2 = o2.getMinLocStart();
				if(comp == 0 && minLocStart1 != null && minLocStart2 != null) {
					comp = Integer.compare(minLocStart1, minLocStart2);
				}
				if(comp == 0) {
					comp = o1.getVariationPkMap().get(Variation.NAME_PROPERTY).compareTo(o2.getVariationPkMap().get(Variation.NAME_PROPERTY));
				}
				return comp;
			}
		};
		Collections.sort(variationScanMemberCounts, comparator);
	}

	
}
