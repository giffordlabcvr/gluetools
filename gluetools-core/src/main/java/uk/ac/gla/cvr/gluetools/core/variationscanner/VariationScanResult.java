package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;

public class VariationScanResult {

	private Variation variation;
	private boolean present;
	private Integer queryNtStart;
	private Integer queryNtEnd;
	
	
	public VariationScanResult(Variation variation, boolean present, Integer queryNtStart, Integer queryNtEnd) {
		super();
		this.variation = variation;
		this.present = present;
		this.queryNtStart = queryNtStart;
		this.queryNtEnd = queryNtEnd;
	}

	public Variation getVariation() {
		return variation;
	}

	public boolean isPresent() {
		return present;
	}

	public Integer getQueryNtStart() {
		return queryNtStart;
	}

	public Integer getQueryNtEnd() {
		return queryNtEnd;
	}

	public static void sortVariationScanResults(List<VariationScanResult> variationScanResults) {
		Comparator<VariationScanResult> comparator = new Comparator<VariationScanResult>(){
			@Override
			public int compare(VariationScanResult o1, VariationScanResult o2) {
				int comp = 0;
				if(comp == 0) {
					comp = Boolean.compare(o1.present, o2.present);
				}
				if(comp == 0 && o1.present) {
					if(comp == 0) {
						comp = Integer.compare(o1.queryNtStart, o2.queryNtStart);
					}
					if(comp == 0) {
						comp = Integer.compare(o1.queryNtEnd, o2.queryNtEnd);
					}
				}
				if(comp == 0) {
					comp = o1.getVariation().getFeatureLoc().getReferenceSequence().getName().compareTo(o2.getVariation().getFeatureLoc().getReferenceSequence().getName());
				}
				if(comp == 0) {
					comp = Integer.compare(o1.getVariation().getRefStart(), o2.getVariation().getRefStart());
				}
				if(comp == 0) {
					comp = Integer.compare(o1.getVariation().getRefEnd(), o2.getVariation().getRefEnd());
				}
				if(comp == 0) {
					comp = o1.getVariation().getFeatureLoc().getFeature().getName().compareTo(o2.getVariation().getFeatureLoc().getFeature().getName());
				}
				if(comp == 0) {
					comp = o1.getVariation().getName().compareTo(o2.getVariation().getName());
				}
				return comp;
			}
		};
		Collections.sort(variationScanResults, comparator);
	}
	
	
	
}
