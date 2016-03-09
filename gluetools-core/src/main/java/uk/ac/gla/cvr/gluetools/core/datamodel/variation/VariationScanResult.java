package uk.ac.gla.cvr.gluetools.core.datamodel.variation;

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

	
	
	
}
