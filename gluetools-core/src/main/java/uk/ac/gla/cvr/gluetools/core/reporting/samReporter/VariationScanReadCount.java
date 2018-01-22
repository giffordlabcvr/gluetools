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
package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;

public class VariationScanReadCount {

	private Map<String,String> variationPkMap;
	private int minLocStart;
	private int maxLocEnd;
	private int readsWherePresent;
	private double pctWherePresent;
	private int readsWhereAbsent;
	private double pctWhereAbsent;
	
	public VariationScanReadCount(Map<String,String> variationPkMap, int minLocStart, int maxLocEnd, int readsWherePresent,
			double pctWherePresent, int readsWhereAbsent, double pctWhereAbsent) {
		super();
		this.variationPkMap = variationPkMap;
		this.minLocStart = minLocStart;
		this.maxLocEnd = maxLocEnd;
		this.readsWherePresent = readsWherePresent;
		this.pctWherePresent = pctWherePresent;
		this.readsWhereAbsent = readsWhereAbsent;
		this.pctWhereAbsent = pctWhereAbsent;
	}
	
	public Map<String, String> getVariationPkMap() {
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

	public int getMinLocStart() {
		return minLocStart;
	}

	public int getMaxLocEnd() {
		return maxLocEnd;
	}

	public int getReadsWherePresent() {
		return readsWherePresent;
	}

	public double getPctWherePresent() {
		return pctWherePresent;
	}

	public int getReadsWhereAbsent() {
		return readsWhereAbsent;
	}

	public double getPctWhereAbsent() {
		return pctWhereAbsent;
	}
	
	public static void sortVariationScanReadCounts(List<VariationScanReadCount> variationScanReadCounts) {
		Comparator<VariationScanReadCount> comparator = new Comparator<VariationScanReadCount>(){
			@Override
			public int compare(VariationScanReadCount o1, VariationScanReadCount o2) {
				int comp = 0;
				
				Integer minLocStart1 = o1.getMinLocStart();
				Integer minLocStart2 = o2.getMinLocStart();
				if(comp == 0 && minLocStart1 != null && minLocStart2 != null) {
					comp = Integer.compare(minLocStart1, minLocStart2);
				}
				Integer maxLocEnd1 = o1.getMaxLocEnd();
				Integer maxLocEnd2 = o2.getMaxLocEnd();
				if(comp == 0 && maxLocEnd1 != null && maxLocEnd2 != null) {
					comp = Integer.compare(maxLocEnd1, maxLocEnd2);
				}
				if(comp == 0) {
					comp = o1.getVariationPkMap().get(Variation.REF_SEQ_NAME_PATH).compareTo(o2.getVariationPkMap().get(Variation.REF_SEQ_NAME_PATH));
				}
				if(comp == 0) {
					comp = o1.getVariationPkMap().get(Variation.FEATURE_NAME_PATH).compareTo(o2.getVariationPkMap().get(Variation.FEATURE_NAME_PATH));
				}
				if(comp == 0) {
					comp = o1.getVariationPkMap().get(Variation.NAME_PROPERTY).compareTo(o2.getVariationPkMap().get(Variation.NAME_PROPERTY));
				}
				return comp;
			}
		};
		Collections.sort(variationScanReadCounts, comparator);
	}

	
	
}
