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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.datamodel.patternlocation.PatternLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.utils.StringUtils;

@PluginClass(elemName="exactMatchNucleotideVariationScanner",
description="Detects exact matches with a pattern when used with a nucleotide Variation")
public class ExactMatchNucleotideVariationScanner extends BaseNucleotideVariationScanner<ExactMatchNucleotideVariationScanner, VariationScanResult>{

	private static ExactMatchNucleotideVariationScanner defaultInstance = new ExactMatchNucleotideVariationScanner();
	
	@Override
	public VariationScanResult scanNucleotides(Variation variation, NtQueryAlignedSegment ntQaSeg) {
		List<PLocScanResult> pLocScanResults = new ArrayList<PLocScanResult>();

		for(int plocIdx = 0; plocIdx < variation.getPatternLocs().size(); plocIdx++) {
			PatternLocation pLoc = variation.getPatternLocs().get(plocIdx);
			PLocScanResult pLocScanResult;

			Integer refStart = pLoc.getRefStart();
			Integer refEnd = pLoc.getRefEnd();
			if(!( refStart >= ntQaSeg.getRefStart() && refEnd <= ntQaSeg.getRefEnd() )) {
				// query segment does not cover pattern loc
				pLocScanResult = new NucleotidePLocScanResult(plocIdx, Collections.emptyList(),
						Collections.emptyList()); // no match in this pattern loc
			} else {
				ReferenceSegment variationRegionSeg = new ReferenceSegment(refStart, refEnd);
				List<NtQueryAlignedSegment> intersection = ReferenceSegment.intersection(Arrays.asList(ntQaSeg), Arrays.asList(variationRegionSeg), 
						ReferenceSegment.cloneLeftSegMerger());
				if(intersection.isEmpty()) {
					// query segment does not cover pattern loc
					pLocScanResult = new NucleotidePLocScanResult(plocIdx, Collections.emptyList(),
							Collections.emptyList()); // no match in this pattern loc
				} else {
					NtQueryAlignedSegment intersectionSeg = intersection.get(0);
					CharSequence nucleotides = intersectionSeg.getNucleotides();
					Integer zeroIndexNtStart = intersectionSeg.getQueryStart();
					if(StringUtils.charSequencesEqual(pLoc.getPattern(), nucleotides)) {
						int ntStart = zeroIndexNtStart;
						int ntEnd = zeroIndexNtStart+nucleotides.length()-1;
						pLocScanResult = new NucleotidePLocScanResult(plocIdx, Arrays.asList(new ReferenceSegment(ntStart, ntEnd)),
								Arrays.asList(nucleotides.toString())); // single match in this pattern loc
					} else {
						pLocScanResult = new NucleotidePLocScanResult(plocIdx, Collections.emptyList(),
								Collections.emptyList()); // no match in this pattern loc
					}
				}
			}
			pLocScanResults.add(pLocScanResult);
		}
		return new VariationScanResult(variation, pLocScanResults);
	}

	public static ExactMatchNucleotideVariationScanner getDefaultInstance() {
		return defaultInstance;
	}

}
