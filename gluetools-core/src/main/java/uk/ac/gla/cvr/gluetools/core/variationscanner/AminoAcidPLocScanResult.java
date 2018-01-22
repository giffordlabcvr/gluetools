/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public class AminoAcidPLocScanResult extends PLocScanResult {

	
	// list of matched AA values within this pattern loc;
	private List<String> aaMatchValues;
	// start codon label for each match
	private List<String> aaStartCodons;
	// end codon label for each match
	private List<String> aaEndCodons;
	
	public AminoAcidPLocScanResult(int index, List<ReferenceSegment> queryLocs, 
			List<String> aaMatchValues, List<String> aaStartCodons, List<String> aaEndCodons) {
		super(index, queryLocs);
		this.aaMatchValues = aaMatchValues;
		this.aaStartCodons = aaStartCodons;
		this.aaEndCodons = aaEndCodons;
		if(aaMatchValues.size() != queryLocs.size()) {
			throw new RuntimeException("aaMatchValues.size() != queryLocs.size()");
		}
		if(aaStartCodons.size() != queryLocs.size()) {
			throw new RuntimeException("aaStartCodons.size() != queryLocs.size()");
		}
		if(aaEndCodons.size() != queryLocs.size()) {
			throw new RuntimeException("aaEndCodons.size() != queryLocs.size()");
		}
	}

	@Override
	public List<String> getMatchedValues() {
		return aaMatchValues;
	}

	public List<String> getAaStartCodons() {
		return aaStartCodons;
	}

	public List<String> getAaEndCodons() {
		return aaEndCodons;
	}
}
