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

import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public class VariationScanResultRow {
	private VariationScanResult vsr;
	private PLocScanResult plsr;
	private ReferenceSegment matchedValueSegment;
	private String matchedValue;
	private String lcStart;
	private String lcEnd;
	

	public VariationScanResultRow(VariationScanResult vsr,
			PLocScanResult plsr, String matchedValue, ReferenceSegment matchedValueSegment, String lcStart, String lcEnd) {
		super();
		this.vsr = vsr;
		this.plsr = plsr;
		this.matchedValue = matchedValue;
		this.matchedValueSegment = matchedValueSegment;
		this.lcStart = lcStart;
		this.lcEnd = lcEnd;
	}

	public VariationScanResultRow(VariationScanResult vsr, PLocScanResult plsr) {
		this(vsr, plsr, null, null, null, null);
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

	public String getLcStart() {
		return lcStart;
	}

	public String getLcEnd() {
		return lcEnd;
	}
	
	
}