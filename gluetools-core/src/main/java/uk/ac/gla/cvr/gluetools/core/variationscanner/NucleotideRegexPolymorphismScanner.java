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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationMetatag.VariationMetatag.VariationMetatagType;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;

public class NucleotideRegexPolymorphismScanner extends BaseNucleotideVariationScanner<NucleotideRegexPolymorphismMatchResult> {

	private static final List<VariationMetatagType> allowedMetatagTypes = 
			Arrays.asList(VariationMetatagType.REGEX_NT_PATTERN);
	private static final List<VariationMetatagType> requiredMetatagTypes = Arrays.asList(VariationMetatagType.REGEX_NT_PATTERN);

	private String regexNtPattern;
	
	public NucleotideRegexPolymorphismScanner() {
		super(allowedMetatagTypes, requiredMetatagTypes);
	}
	
	@Override
	protected void init(CommandContext cmdContext) {
		super.init(cmdContext);		
		this.regexNtPattern = getStringMetatagValue(VariationMetatagType.REGEX_NT_PATTERN);
	}

	@Override
	protected VariationScanResult<NucleotideRegexPolymorphismMatchResult> scanInternal(
			CommandContext cmdContext, 
			List<QueryAlignedSegment> queryToRefSegs,
			String queryNts, String qualityString) {
		List<NucleotideRegexPolymorphismMatchResult> matchResults = new ArrayList<NucleotideRegexPolymorphismMatchResult>();
		boolean sufficientCoverage = computeSufficientCoverage(queryToRefSegs);
		Variation variation = getVariation();

		List<QueryAlignedSegment> queryToRefSegsVariationRegion = 
				ReferenceSegment.intersection(queryToRefSegs, Arrays.asList(new ReferenceSegment(variation.getRefStart(), variation.getRefEnd())), 
						ReferenceSegment.cloneLeftSegMerger());

		Pattern pattern = parseRegex(regexNtPattern);

		if(sufficientCoverage) {
			for(QueryAlignedSegment qaSeg: queryToRefSegsVariationRegion) {
				Matcher matcher = pattern.matcher(SegmentUtils.base1SubString(queryNts, qaSeg.getQueryStart(), qaSeg.getQueryEnd()));
				while(matcher.find()) {
					int queryNtStart = qaSeg.getQueryStart() + matcher.start();
					int queryNtEnd = qaSeg.getQueryStart() + matcher.end()-1;
					int refNtStart = queryNtStart + qaSeg.getQueryToReferenceOffset();
					int refNtEnd = queryNtEnd + qaSeg.getQueryToReferenceOffset();
					String polymorphismQueryNts = matcher.group();
					NucleotideRegexPolymorphismMatchResult nrpmr = 
							new NucleotideRegexPolymorphismMatchResult(refNtStart, refNtEnd, queryNtStart, queryNtEnd, polymorphismQueryNts);
					if(qualityString != null) {
						nrpmr.setWorstContributingQScore(SamUtils.worstQScore(qualityString, queryNtStart, queryNtEnd));
					}
					matchResults.add(nrpmr);
				}
			}

		}
		VariationScanResult<NucleotideRegexPolymorphismMatchResult> variationScanResult = new VariationScanResult<NucleotideRegexPolymorphismMatchResult>(this, sufficientCoverage, matchResults);
		if(sufficientCoverage && qualityString != null) {
			if(matchResults.isEmpty()) {
				variationScanResult.setQScore(worstQScoreOfSegments(qualityString, queryToRefSegsVariationRegion));
			} else {
				variationScanResult.setQScore(bestQScoreOfMatchResults(matchResults));
			}
		}
		return variationScanResult;

	}
	
}
