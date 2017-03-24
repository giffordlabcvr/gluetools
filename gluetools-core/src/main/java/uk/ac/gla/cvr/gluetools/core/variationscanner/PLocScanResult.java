package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public abstract class PLocScanResult {

	private int index;
	private List<ReferenceSegment> queryLocs;

	// One PLocScanResult is created for each PLoc in a variation when the variation is scanned.
	// Each PLocScanResult contains one ReferenceSegment for each match within the PLoc, 
	// which is based on the NT coordinates of the matches in the query.
	protected PLocScanResult(int index, List<ReferenceSegment> queryLocs) {
		super();
		this.index = index;
		this.queryLocs = queryLocs;
	}

	public List<ReferenceSegment> getQueryLocs() {
		return queryLocs;
	}
	
	public int getIndex() {
		return index;
	}

	public abstract List<String> getMatchedValues();
	
}
