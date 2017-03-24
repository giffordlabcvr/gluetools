package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public class NucleotidePLocScanResult extends PLocScanResult {

	// list of matched NT values within this pattern loc;
	private List<String> ntMatchValues;
	
	public NucleotidePLocScanResult(int index, List<ReferenceSegment> queryLocs, List<String> ntMatchValues) {
		super(index, queryLocs);
		this.ntMatchValues = ntMatchValues;
		if(ntMatchValues.size() != queryLocs.size()) {
			throw new RuntimeException("ntMatchValues.size() != queryLocs.size()");
		}
	}

	@Override
	public List<String> getMatchedValues() {
		return ntMatchValues;
	}
	
}
