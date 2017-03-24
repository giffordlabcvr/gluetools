package uk.ac.gla.cvr.gluetools.core.variationscanner;

import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public class VariationScanResultRow {
	private VariationScanResult vsr;
	private PLocScanResult plsr;
	private ReferenceSegment matchedValueSegment;
	private String matchedValue;
	

	public VariationScanResultRow(VariationScanResult vsr,
			PLocScanResult plsr, String matchedValue, ReferenceSegment matchedValueSegment) {
		super();
		this.vsr = vsr;
		this.plsr = plsr;
		this.matchedValue = matchedValue;
		this.matchedValueSegment = matchedValueSegment;
	}

	public VariationScanResultRow(VariationScanResult vsr, PLocScanResult plsr) {
		this(vsr, plsr, null, null);
	}

	public VariationScanResultRow(VariationScanResult vsr) {
		this(vsr, null);
	}

	public VariationScanResult getVsr() {
		return vsr;
	}

	public PLocScanResult getPlsr() {
		return plsr;
	}

	public ReferenceSegment getMatchedValueSegment() {
		return matchedValueSegment;
	}

	public String getMatchedValue() {
		return matchedValue;
	}
	
	
}