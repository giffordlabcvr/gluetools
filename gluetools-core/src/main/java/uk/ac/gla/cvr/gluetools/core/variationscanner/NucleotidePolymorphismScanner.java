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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import uk.ac.gla.cvr.gluetools.core.datamodel.patternlocation.PatternLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationMetatag.VariationMetatag.VariationMetatagType;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public class NucleotidePolymorphismScanner extends BaseNucleotideVariationScanner<NucleotidePolymorphismMatchResult> {

	private static final List<VariationMetatagType> allowedMetatagTypes = Arrays.asList();
	private static final List<VariationMetatagType> requiredMetatagTypes = Arrays.asList();

	public NucleotidePolymorphismScanner() {
		super(allowedMetatagTypes, requiredMetatagTypes);
	}

	@Override
	public VariationScanResult scanNucleotides(Variation variation, NtQueryAlignedSegment ntQaSeg) {
		List<PLocScanResult> pLocScanResults = new ArrayList<PLocScanResult>();

		for(int plocIdx = 0; plocIdx < variation.getPatternLocs().size(); plocIdx++) {
			PatternLocation pLoc = variation.getPatternLocs().get(plocIdx);
			PLocScanResult pLocScanResult;

			Integer refStart = pLoc.getRefStart();
			Integer refEnd = pLoc.getRefEnd();
			if(!( refStart >= ntQaSeg.getRefStart() && refEnd <= ntQaSeg.getRefEnd() )) {
				pLocScanResult = new NucleotidePLocScanResult(plocIdx, Collections.emptyList(),
						Collections.emptyList()); // no match in this pattern loc
			} else {
				ReferenceSegment variationRegionSeg = new ReferenceSegment(refStart, refEnd);
				List<NtQueryAlignedSegment> intersection = ReferenceSegment.intersection(Arrays.asList(ntQaSeg), Arrays.asList(variationRegionSeg), 
						ReferenceSegment.cloneLeftSegMerger());
				if(intersection.isEmpty()) {
					pLocScanResult = new NucleotidePLocScanResult(plocIdx, Collections.emptyList(),
							Collections.emptyList()); // no match in this pattern loc
				} else {
					// set up caching of Pattern in PatternLoc.
					Pattern regexPattern = (Pattern) pLoc.getScannerData("NT_REGEX_PATTERN");
					if(regexPattern == null) {
						regexPattern = parseRegex(pLoc);
						pLoc.setScannerData("NT_REGEX_PATTERN", regexPattern);
					}
					
					NtQueryAlignedSegment intersectionSeg = intersection.get(0);
					CharSequence nucleotides = intersectionSeg.getNucleotides();
					Integer zeroIndexNtStart = intersectionSeg.getQueryStart();
		
					List<ReferenceSegment> queryLocs = new ArrayList<ReferenceSegment>();
					List<String> ntMatchValues = new ArrayList<String>();
					
					Matcher matcher = regexPattern.matcher(nucleotides);
					while(matcher.find()) {
						int ntStart = zeroIndexNtStart + matcher.start();
						int ntEnd = zeroIndexNtStart + matcher.end() - 1;
						queryLocs.add(new ReferenceSegment(ntStart, ntEnd));
						ntMatchValues.add(matcher.group());
					} 
					pLocScanResult = new NucleotidePLocScanResult(plocIdx, queryLocs, ntMatchValues);
				}
			}
			pLocScanResults.add(pLocScanResult);
		}
		return new VariationScanResult(variation, pLocScanResults);
	}

	private Pattern parseRegex(PatternLocation pLoc) {
		try {
			return Pattern.compile(pLoc.getPattern());
		} catch(PatternSyntaxException pse) {
			Variation variation = pLoc.getVariation();
			throw new VariationException(pse, Code.VARIATION_SCANNER_EXCEPTION, 
					variation.getFeatureLoc().getReferenceSequence().getName(), 
					variation.getFeatureLoc().getFeature().getName(), variation.getName(), 
					"Syntax error in variation regex: "+pse.getMessage());
		}
	}
	
}
