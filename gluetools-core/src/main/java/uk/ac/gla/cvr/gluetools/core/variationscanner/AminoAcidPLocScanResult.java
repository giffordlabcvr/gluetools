package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public class AminoAcidPLocScanResult extends PLocScanResult {

	
	// list of matched AA values within this pattern loc;
	private List<String> aaMatchValues;
	/*
	 // start codon label for each match
	 private List<String> aaStartCodons;
	 // end codon label for each match
	 private List<String> aaEndCodons;
	*/
	
	public AminoAcidPLocScanResult(int index, List<ReferenceSegment> queryLocs, List<String> aaMatchValues/*,
			List<String> aaStartCodons, List<String> aaEndCodons*/) {
		super(index, queryLocs);
		this.aaMatchValues = aaMatchValues;
		/*
		this.aaStartCodons = aaStartCodons;
		this.aaEndCodons = aaEndCodons;
		*/
		if(aaMatchValues.size() != queryLocs.size()) {
			throw new RuntimeException("aaMatchValues.size() != queryLocs.size()");
		}
		/*
		if(aaStartCodons.size() != queryLocs.size()) {
			throw new RuntimeException("aaStartCodons.size() != queryLocs.size()");
		}
		if(aaEndCodons.size() != queryLocs.size()) {
			throw new RuntimeException("aaEndCodons.size() != queryLocs.size()");
		}
		*/
	}

	@Override
	public List<String> getMatchedValues() {
		return aaMatchValues;
	}

	/*
	public List<String> getAaStartCodons() {
		return aaStartCodons;
	}

	public List<String> getAaEndCodons() {
		return aaEndCodons;
	}
	*/

}
