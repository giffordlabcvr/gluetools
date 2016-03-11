package uk.ac.gla.cvr.gluetools.core.datamodel.variation;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class VariationScanResult {

	private Variation variation;
	private boolean present;
	private boolean absent;
	
	public VariationScanResult(Variation variation, boolean present, boolean absent) {
		super();
		this.variation = variation;
		this.present = present;
		this.absent = absent;
	}

	public Variation getVariation() {
		return variation;
	}

	public boolean isPresent() {
		return present;
	}

	public boolean isAbsent() {
		return absent;
	}

	public static void sortVariationScanResults(List<VariationScanResult> variationScanResults) {
		Comparator<VariationScanResult> comparator = new Comparator<VariationScanResult>(){
			@Override
			public int compare(VariationScanResult o1, VariationScanResult o2) {
				int comp = Integer.compare(o1.getVariation().getRefStart(), o2.getVariation().getRefStart());
				if(comp == 0) {
					comp = o1.getVariation().getName().compareTo(o2.getVariation().getName());
				}
				return comp;
			}
		};
		Collections.sort(variationScanResults, comparator);
	}
	
	
	
}
