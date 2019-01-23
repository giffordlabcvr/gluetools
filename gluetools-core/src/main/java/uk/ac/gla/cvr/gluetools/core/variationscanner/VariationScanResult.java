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
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation.VariationType;
import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;

public class VariationScanResult<M extends VariationScannerMatchResult> {

	private Map<String,String> variationPkMap;
	private Integer refStart, refEnd;
	// sufficientCoverage == true iff the query had sufficient nucleotide coverage of the 
	// reference region to scan for the variation.
	private boolean sufficientCoverage; 
	private boolean present; 
	private String variationRenderedName;
	private VariationType variationType;
	
	private Integer qScore;
	
	private List<M> scannerMatchResults;
	private BaseVariationScanner<M> scanner;

	public VariationScanResult(BaseVariationScanner<M> scanner, boolean sufficientCoverage, List<M> scannerMatchResults) {
		this(scanner, scanner.getVariation().getRefStart(), scanner.getVariation().getRefEnd(), sufficientCoverage, scannerMatchResults);
	}
		

	public VariationScanResult(BaseVariationScanner<M> scanner, Integer refStart, Integer refEnd, boolean sufficientCoverage, 
			List<M> scannerMatchResults) {
		// by default "is present" true iff there are matches.
		// counterexample is conjunctions, where we record a match result containing partial match.
		this(scanner, refStart, refEnd, sufficientCoverage, scannerMatchResults, !scannerMatchResults.isEmpty());
	}
		
	public VariationScanResult(BaseVariationScanner<M> scanner, Integer refStart, Integer refEnd, boolean sufficientCoverage, 
			List<M> scannerMatchResults, boolean isPresent) {
		super();
		this.scanner = scanner;
		this.variationPkMap = scanner.getVariation().pkMap();
		this.refStart = refStart;
		this.refEnd = refEnd;
		this.sufficientCoverage = sufficientCoverage;
		this.scannerMatchResults = scannerMatchResults;
		this.present = isPresent;
		this.variationRenderedName = scanner.getVariation().getRenderedName();
		this.variationType = scanner.getVariation().getVariationType();
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

	public VariationType getVariationType() {
		return variationType;
	}

	public String getVariationRenderedName() {
		return variationRenderedName;
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
					
					Integer o1qStart = o1.computeMinMatchStart();
					Integer o2qStart = o2.computeMinMatchStart();
					
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
	


	public Integer getQScore() {
		return qScore;
	}


	public void setQScore(Integer qScore) {
		this.qScore = qScore;
	}


	protected Integer leastBadQScoreOfMatches(List<M> matchResults) {
		Integer leastBadMatchQScore = null;
		for(M matchResult: matchResults) {
			Integer matchWorstQScore = matchResult.getWorstContributingQScore();
			if(matchWorstQScore != null) {
				if(leastBadMatchQScore == null) {
					leastBadMatchQScore = matchWorstQScore;
				} else {
					leastBadMatchQScore = Math.max(leastBadMatchQScore, matchWorstQScore);
				}
			}
		}
		return leastBadMatchQScore;
	}

	private Integer computeMinMatchStart() {
		if(scannerMatchResults.isEmpty()) {
			return null;
		}
		int minMatchStart = Integer.MAX_VALUE;
		for(VariationScannerMatchResult scannerMatchResult: scannerMatchResults) {
			minMatchStart = Math.min(minMatchStart, scannerMatchResult.getRefStart());
		}
		return minMatchStart;
	}

	public Integer getRefStart() {
		return refStart;
	}

	public Integer getRefEnd() {
		return refEnd;
	}


	public BaseVariationScanner<M> getScanner() {
		return scanner;
	}


	public static void variationScanResultAsCommandObject(CommandObject cmdObject, VariationScanResult<?> vsr) {
		cmdObject.set("referenceName", vsr.getVariationReferenceName());
		cmdObject.set("featureName", vsr.getVariationFeatureName());
		cmdObject.set("variationName", vsr.getVariationName());
		cmdObject.set("variationType", vsr.getVariationType().name());
		cmdObject.set("present", vsr.isPresent());
		cmdObject.set("sufficientCoverage", vsr.isSufficientCoverage());
		CommandArray matchesArray = cmdObject.setArray("matches");
		for(VariationScannerMatchResult vsmr: vsr.getVariationScannerMatchResults()) {
			CommandObject matchObject = matchesArray.addObject();
			vsmr.populateMatchObject(matchObject);
		}
	}

}
