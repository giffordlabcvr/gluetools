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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;

public class VariationScanResult<M extends VariationScannerMatchResult> {

	private Map<String,String> variationPkMap;
	private int refStart, refEnd;
	// sufficientCoverage == true iff the query had sufficient nucleotide coverage of the 
	// reference region to scan for the variation.
	private boolean sufficientCoverage; 
	private boolean present; // true iff any matches were found.
	private String variationRenderedName;
	
	private List<M> scannerMatchResults;
	
	public VariationScanResult(Variation variation, boolean sufficientCoverage, List<M> scannerMatchResults) {
		super();
		this.variationPkMap = variation.pkMap();
		this.refStart = variation.getRefStart();
		this.refEnd = variation.getRefEnd();
		this.sufficientCoverage = sufficientCoverage;
		this.present = true;
		this.scannerMatchResults = scannerMatchResults;
		if(this.scannerMatchResults.isEmpty()) {
			this.present = false;
		}
		this.variationRenderedName = variation.getRenderedName();
	}

	public Map<String,String> getVariationPkMap() {
		return variationPkMap;
	}

	public String getVariationReferenceName() {
		return variationPkMap.get(Variation.REF_SEQ_NAME_PATH);
	}

	public String getVariationFeatureName() {
		return variationPkMap.get(Variation.FEATURE_NAME_PATH);
	}

	public String getVariationName() {
		return variationPkMap.get(Variation.NAME_PROPERTY);
	}

	public String getVariationRenderedName() {
		return variationRenderedName;
	}

	public int getMinLocStart() {
		return refStart;
	}

	public int getMaxLocEnd() {
		return refEnd;
	}

	public boolean isPresent() {
		return present;
	}

	public boolean isSufficientCoverage() {
		return sufficientCoverage;
	}

	public List<M> getVariationScannerMatchResults() {
		return scannerMatchResults;
	}
	

	public static void sortVariationScanResults(List<VariationScanResult<?>> variationScanResults) {
		Comparator<VariationScanResult<?>> comparator = new Comparator<VariationScanResult<?>>(){
			@Override
			public int compare(VariationScanResult<?> o1, VariationScanResult<?> o2) {
				int comp = 0;
				if(comp == 0) {
					comp = Boolean.compare(o1.present, o2.present);
				}
				if(comp == 0 && o1.present) {
					
					Integer o1qStart = o1.minRefStart();
					Integer o2qStart = o2.minRefStart();
					
					if(comp == 0 && o1qStart != null && o2qStart != null) {
						comp = Integer.compare(o1qStart, o2qStart);
					}
				}
				if(comp == 0) {
					comp = o1.getVariationReferenceName().compareTo(o2.getVariationReferenceName());
				}
				Integer vLocStart1 = o1.refStart;
				Integer vLocStart2 = o2.refStart;
				if(comp == 0 && vLocStart1 != null && vLocStart2 != null) {
					comp = Integer.compare(vLocStart1, vLocStart2);
				}
				Integer vLocEnd1 = o1.refEnd;
				Integer vLocEnd2 = o2.refEnd;
				if(comp == 0 && vLocEnd1 != null && vLocEnd2 != null) {
					comp = Integer.compare(vLocEnd1, vLocEnd2);
				}
				if(comp == 0) {
					comp = o1.getVariationFeatureName().compareTo(o2.getVariationFeatureName());
				}
				if(comp == 0) {
					comp = o1.getVariationName().compareTo(o2.getVariationName());
				}
				return comp;
			}
		};
		Collections.sort(variationScanResults, comparator);
	}

	private int minRefStart() {
		if(scannerMatchResults.isEmpty()) {
			return 0;
		}
		int minRefStart = Integer.MAX_VALUE;
		for(VariationScannerMatchResult scannerMatchResult: scannerMatchResults) {
			minRefStart = Math.min(minRefStart, scannerMatchResult.getRefStart());
		}
		return minRefStart;
	}
	
}
