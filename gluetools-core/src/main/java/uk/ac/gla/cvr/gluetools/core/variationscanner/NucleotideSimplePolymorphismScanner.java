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

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationMetatag.VariationMetatag.VariationMetatagType;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.core.translation.ResidueUtils;

public class NucleotideSimplePolymorphismScanner extends BaseNucleotideVariationScanner<NucleotideSimplePolymorphismMatchResult> {

	private static final List<VariationMetatagType> allowedMetatagTypes = 
			Arrays.asList(VariationMetatagType.SIMPLE_NT_PATTERN, 
					VariationMetatagType.MIN_COMBINED_NT_FRACTION);
	private static final List<VariationMetatagType> requiredMetatagTypes = 
			Arrays.asList(VariationMetatagType.SIMPLE_NT_PATTERN);

	private String simpleNtPattern;
	private Double minCombinedNtFraction;
	
	public NucleotideSimplePolymorphismScanner() {
		super(allowedMetatagTypes, requiredMetatagTypes);
	}
	
	@Override
	protected void init(CommandContext cmdContext) {
		super.init(cmdContext);		
		this.simpleNtPattern = getStringMetatagValue(VariationMetatagType.SIMPLE_NT_PATTERN);
		this.minCombinedNtFraction = getDoubleMetatagValue(VariationMetatagType.MIN_COMBINED_NT_FRACTION);
		if(this.minCombinedNtFraction == null) {
			this.minCombinedNtFraction = 1.0;
		}
	}


	@Override
	protected VariationScanResult<NucleotideSimplePolymorphismMatchResult> scanInternal(
			CommandContext cmdContext, 
			List<QueryAlignedSegment> queryToRefSegs,
			String queryNts, String qualityString) {
		List<NucleotideSimplePolymorphismMatchResult> matchResults = new ArrayList<NucleotideSimplePolymorphismMatchResult>();
		boolean sufficientCoverage = computeSufficientCoverage(queryToRefSegs);

		Variation variation = getVariation();

		List<QueryAlignedSegment> queryToRefSegsVariationRegion = 
				ReferenceSegment.intersection(queryToRefSegs, Arrays.asList(new ReferenceSegment(variation.getRefStart(), variation.getRefEnd())), 
						ReferenceSegment.cloneLeftSegMerger());


		if(sufficientCoverage) {
			for(QueryAlignedSegment qaSeg: queryToRefSegsVariationRegion) {
				String segNts = SegmentUtils.base1SubString(queryNts, qaSeg.getQueryStart(), qaSeg.getQueryEnd());
				int nextIndex = 0;
				AmbigNtsMatch ambigNtsMatch;
				do {
					ambigNtsMatch = ambigNtsMatch(segNts, nextIndex, simpleNtPattern, minCombinedNtFraction);
					if(ambigNtsMatch != null) {
						int queryNtStart = qaSeg.getQueryStart() + ambigNtsMatch.index;
						int queryNtEnd = queryNtStart + (simpleNtPattern.length()-1);
						int refNtStart = queryNtStart + qaSeg.getQueryToReferenceOffset();
						int refNtEnd = queryNtEnd + qaSeg.getQueryToReferenceOffset();
						String polymorphismQueryNts = ambigNtsMatch.queryNts.toString();
						NucleotideSimplePolymorphismMatchResult nspmr = 
								new NucleotideSimplePolymorphismMatchResult(refNtStart, refNtEnd, 
										queryNtStart, queryNtEnd, 
										polymorphismQueryNts, ambigNtsMatch.combinedNtFraction);
						if(qualityString != null) {
							nspmr.setWorstContributingQScore(SamUtils.worstQScore(qualityString, queryNtStart, queryNtEnd));
						}
						matchResults.add(nspmr);
						nextIndex = ambigNtsMatch.index+1;
					}
				} while(ambigNtsMatch != null);
			}
		}
		VariationScanResult<NucleotideSimplePolymorphismMatchResult> variationScanResult = new VariationScanResult<NucleotideSimplePolymorphismMatchResult>(this, sufficientCoverage, matchResults);
		if(sufficientCoverage && qualityString != null) {
			if(matchResults.isEmpty()) {
				variationScanResult.setQScore(worstQScoreOfSegments(qualityString, queryToRefSegsVariationRegion));
			} else {
				variationScanResult.setQScore(bestQScoreOfMatchResults(matchResults));
			}
		}
		return variationScanResult;

	}
	
	private static class AmbigNtsMatch {
		int index;
		StringBuffer queryNts = new StringBuffer();
		double combinedNtFraction = 1.0;
	}
	
	private AmbigNtsMatch ambigNtsMatch(String queryAmbigNts, int fromIndex, String pattern, double minCombinedNtFraction) {
		for(int startIndex = fromIndex; startIndex < (queryAmbigNts.length() - pattern.length()) + 1; startIndex++) {
			AmbigNtsMatch ambigNtsMatch = new AmbigNtsMatch();
			ambigNtsMatch.index = startIndex;
			boolean match = true;
			for(int i = 0; i < pattern.length(); i++) {
				char concreteNt = pattern.charAt(i);
				char ambigNt = queryAmbigNts.charAt(startIndex+i);
				double ntFraction = ResidueUtils
						.ambigNtToConcreteNtProb(ResidueUtils.ambigNtToInt(ambigNt), 
								ResidueUtils.concreteNtToInt(concreteNt));
				ambigNtsMatch.combinedNtFraction *= ntFraction;
				ambigNtsMatch.queryNts.append(ambigNt);
				if(ambigNtsMatch.combinedNtFraction < minCombinedNtFraction) {
					match = false;
					break;
				}
			}
			if(match) {
				return ambigNtsMatch;
			}
		}
		return null;
	}
	
	
}


