package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public class ComparisonAminoAcidVariationScanResult extends VariationScanResult {

	private PLocScanResult[] plocScanResults;
	
	public ComparisonAminoAcidVariationScanResult(Variation variation, List<ReferenceSegment> queryMatchLocations) {
		super(variation, true, queryMatchLocations);
		this.plocScanResults = new PLocScanResult[variation.getPatternLocs().size()];
	}

	public PLocScanResult[] getPLocScanResults() {
		return plocScanResults;
	}
	
	public static class PLocScanResult {
		private String queryAminoAcids;
		
		public PLocScanResult(String queryAminoAcids) {
			super();
			this.queryAminoAcids = queryAminoAcids;
		}
	}
}
