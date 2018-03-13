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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationMetatag.VariationMetatag.VariationMetatagType;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public class NucleotidePolymorphismScanner extends BaseNucleotideVariationScanner<NucleotidePolymorphismMatchResult> {

	private static final List<VariationMetatagType> allowedMetatagTypes = 
			Arrays.asList(VariationMetatagType.SIMPLE_NT_PATTERN, 
							VariationMetatagType.REGEX_NT_PATTERN);
	private static final List<VariationMetatagType> requiredMetatagTypes = Arrays.asList();

	public NucleotidePolymorphismScanner() {
		super(allowedMetatagTypes, requiredMetatagTypes);
	}

	@Override
	public void validate() {
		super.validate();
		Map<VariationMetatagType, String> metatagsMap = getMetatagsMap();
		if(metatagsMap.containsKey(VariationMetatagType.SIMPLE_NT_PATTERN) &&
				metatagsMap.containsKey(VariationMetatagType.REGEX_NT_PATTERN)) {
			throwScannerException("Only one of SIMPLE_NT_PATTERN and REGEX_NT_PATTERN may be defined");
		}
		if((!metatagsMap.containsKey(VariationMetatagType.SIMPLE_NT_PATTERN)) &&
				!metatagsMap.containsKey(VariationMetatagType.REGEX_NT_PATTERN)) {
			throwScannerException("At least one of SIMPLE_NT_PATTERN and REGEX_NT_PATTERN must be defined");
		}
	}

	


	@Override
	public VariationScanResult<NucleotidePolymorphismMatchResult> scan(
			CommandContext cmdContext,
			List<NtQueryAlignedSegment> queryToRefNtSegs) {
		List<NucleotidePolymorphismMatchResult> matchResults = new ArrayList<NucleotidePolymorphismMatchResult>();
		boolean sufficientCoverage = computeSufficientCoverage(queryToRefNtSegs);
		if(sufficientCoverage) {
			Variation variation = getVariation();

			List<NtQueryAlignedSegment> queryToRefNtSegsVariationRegion = 
					ReferenceSegment.intersection(queryToRefNtSegs, Arrays.asList(new ReferenceSegment(variation.getRefStart(), variation.getRefEnd())), 
							ReferenceSegment.cloneLeftSegMerger());


			Map<VariationMetatagType, String> metatagsMap = getMetatagsMap();
			String simpleNtPattern = metatagsMap.get(VariationMetatagType.SIMPLE_NT_PATTERN);
			String regexNtPattern = metatagsMap.get(VariationMetatagType.REGEX_NT_PATTERN);
			if(simpleNtPattern != null) {
				for(NtQueryAlignedSegment ntQaSeg: queryToRefNtSegsVariationRegion) {
					String segNts = ntQaSeg.getNucleotides().toString();
					int nextIndex = -1;
					do {
						nextIndex = segNts.indexOf(simpleNtPattern, nextIndex+1);
						if(nextIndex >= 0) {
							int queryNtStart = ntQaSeg.getQueryStart() + nextIndex;
							int queryNtEnd = queryNtStart + (simpleNtPattern.length()-1);
							int refNtStart = queryNtStart + ntQaSeg.getQueryToReferenceOffset();
							int refNtEnd = queryNtEnd + ntQaSeg.getQueryToReferenceOffset();
							String queryNts = segNts.substring(nextIndex, nextIndex+simpleNtPattern.length());
							NucleotidePolymorphismMatchResult npmr = 
									new NucleotidePolymorphismMatchResult(refNtStart, refNtEnd, queryNtStart, queryNtEnd, queryNts);
							matchResults.add(npmr);
						}
					} while(nextIndex != -1);
				}
			} else if(regexNtPattern != null) {
				Pattern pattern = parseRegex(regexNtPattern);
				for(NtQueryAlignedSegment ntQaSeg: queryToRefNtSegsVariationRegion) {
					Matcher matcher = pattern.matcher(ntQaSeg.getNucleotides());
					while(matcher.find()) {
						int queryNtStart = ntQaSeg.getQueryStart() + matcher.start();
						int queryNtEnd = ntQaSeg.getQueryStart() + matcher.end()-1;
						int refNtStart = queryNtStart + ntQaSeg.getQueryToReferenceOffset();
						int refNtEnd = queryNtEnd + ntQaSeg.getQueryToReferenceOffset();
						String queryNts = matcher.group();
						NucleotidePolymorphismMatchResult npmr = 
								new NucleotidePolymorphismMatchResult(refNtStart, refNtEnd, queryNtStart, queryNtEnd, queryNts);
						matchResults.add(npmr);
					}
				}
			} else {
				throwScannerException("Neither SIMPLE_NT_PATTERN nor REGEX_NT_PATTERN metatags are defined");
			}
		}
		return new VariationScanResult<NucleotidePolymorphismMatchResult>(getVariation(), sufficientCoverage, matchResults);

	}
	
}
