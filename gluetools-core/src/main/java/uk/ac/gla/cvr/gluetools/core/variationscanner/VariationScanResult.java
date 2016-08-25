package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public class VariationScanResult {

	private Variation variation;
	private boolean present;
	
	private List<ReferenceSegment> queryMatchLocations = new ArrayList<ReferenceSegment>();
	
	public VariationScanResult(Variation variation, boolean present, List<ReferenceSegment> queryMatchLocations) {
		super();
		this.variation = variation;
		this.present = present;
		this.queryMatchLocations = queryMatchLocations;
	}

	public Variation getVariation() {
		return variation;
	}

	public boolean isPresent() {
		return present;
	}

	public List<ReferenceSegment> getQueryMatchLocations() {
		return queryMatchLocations;
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
					
					Integer o1qStart = ReferenceSegment.minRefStart(o1.queryMatchLocations);
					Integer o2qStart = ReferenceSegment.minRefStart(o2.queryMatchLocations);
					
					if(comp == 0 && o1qStart != null && o2qStart != null) {
						comp = Integer.compare(o1qStart, o2qStart);
					}

					Integer o1qEnd = ReferenceSegment.maxRefEnd(o1.queryMatchLocations);
					Integer o2qEnd = ReferenceSegment.maxRefEnd(o2.queryMatchLocations);

					if(comp == 0) {
						comp = Integer.compare(o1qEnd, o2qEnd);
					}
				}
				if(comp == 0) {
					comp = o1.getVariation().getFeatureLoc().getReferenceSequence().getName().compareTo(o2.getVariation().getFeatureLoc().getReferenceSequence().getName());
				}
				Integer vLocStart1 = o1.getVariation().minLocStart();
				Integer vLocStart2 = o2.getVariation().minLocStart();
				if(comp == 0 && vLocStart1 != null && vLocStart2 != null) {
					comp = Integer.compare(vLocStart1, vLocStart2);
				}
				Integer vLocEnd1 = o1.getVariation().maxLocEnd();
				Integer vLocEnd2 = o2.getVariation().maxLocEnd();
				if(comp == 0 && vLocEnd1 != null && vLocEnd2 != null) {
					comp = Integer.compare(vLocEnd1, vLocEnd2);
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
