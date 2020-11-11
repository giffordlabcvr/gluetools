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

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SimpleNucleotideContentProvider;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationMetatag.VariationMetatag.VariationMetatagType;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.core.translation.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;

public class AminoAcidRegexPolymorphismScanner extends BaseAminoAcidVariationScanner<AminoAcidRegexPolymorphismMatchResult> {

	private static final List<VariationMetatagType> allowedMetatagTypes = 
			Arrays.asList(VariationMetatagType.REGEX_AA_PATTERN,
					VariationMetatagType.ALLOW_PARTIAL_COVERAGE);
	private static final List<VariationMetatagType> requiredMetatagTypes = Arrays.asList(VariationMetatagType.REGEX_AA_PATTERN);

	private Pattern regexAaPattern;
	private Boolean allowPartialCoverage;
	private Translator translator;

	public AminoAcidRegexPolymorphismScanner() {
		super(allowedMetatagTypes, requiredMetatagTypes);
	}
	
	@Override
	protected void init(CommandContext cmdContext) {
		super.init(cmdContext);		
		String patternMetatagValue = getStringMetatagValue(VariationMetatagType.REGEX_AA_PATTERN);
		if(patternMetatagValue != null) {
			this.regexAaPattern = parseRegex(patternMetatagValue);
		}
		Boolean configuredAllowPartialCoverage = getBooleanMetatagValue(VariationMetatagType.ALLOW_PARTIAL_COVERAGE);
		if(configuredAllowPartialCoverage != null) {
			this.allowPartialCoverage = configuredAllowPartialCoverage;
		} else {
			this.allowPartialCoverage = false;
		}
		this.translator = new CommandContextTranslator(cmdContext);
	}
	
	@Override
	protected boolean computeSufficientCoverage(List<QueryAlignedSegment> queryToRefSegs) {
		if(this.allowPartialCoverage) {
			return true;
		}
		return super.computeSufficientCoverage(queryToRefSegs);
	}

	@Override
	protected VariationScanResult<AminoAcidRegexPolymorphismMatchResult> scanInternal(
			CommandContext cmdContext, 
			List<QueryAlignedSegment> queryToRefSegs,
			String queryNts, String qualityString) {
		List<AminoAcidRegexPolymorphismMatchResult> matchResults = new ArrayList<AminoAcidRegexPolymorphismMatchResult>();
		boolean sufficientCoverage = computeSufficientCoverage(queryToRefSegs);
		Variation variation = getVariation();

		List<QueryAlignedSegment> queryToRefSegsVariationRegion = 
				ReferenceSegment.intersection(queryToRefSegs, Arrays.asList(new ReferenceSegment(variation.getRefStart(), variation.getRefEnd())), 
						ReferenceSegment.cloneLeftSegMerger());

		List<LabeledQueryAminoAcid> queryLqaas = variation.getFeatureLoc().translateQueryNucleotides(cmdContext, translator, queryToRefSegsVariationRegion, 
				new SimpleNucleotideContentProvider(queryNts));
		
		List<List<LabeledQueryAminoAcid>> contiguousLqaaSections = LabeledQueryAminoAcid.findContiguousLqaaSections(queryLqaas);

		if(sufficientCoverage) {
			for(List<LabeledQueryAminoAcid> contiguousLqaaSection: contiguousLqaaSections) {
				
				char[] segAaChars = new char[contiguousLqaaSection.size()];
				for(int i = 0; i < contiguousLqaaSection.size(); i++) {
					segAaChars[i] = contiguousLqaaSection.get(i).getLabeledAminoAcid().getTranslationInfo().getSingleCharTranslation();
				}
				String segAas = new String(segAaChars);
				Matcher matcher = regexAaPattern.matcher(segAas);
				while(matcher.find()) {
					int matcherStart = matcher.start();
					int matcherEnd = matcher.end();
					LabeledQueryAminoAcid matchStartLqaa = contiguousLqaaSection.get(matcherStart);
					LabeledQueryAminoAcid matchEndLqaa = contiguousLqaaSection.get(matcherEnd-1);
					LabeledCodon firstLabeledCodon = matchStartLqaa.getLabeledAminoAcid().getLabeledCodon();
					LabeledCodon lastLabeledCodon = matchEndLqaa.getLabeledAminoAcid().getLabeledCodon();

					int queryNtStart = matchStartLqaa.getQueryNtStart();
					int queryNtEnd = matchEndLqaa.getQueryNtEnd();
					int refNtStart = firstLabeledCodon.getNtStart();
					int refNtEnd = lastLabeledCodon.getNtEnd();
					
					String polymorphismQueryNts = SegmentUtils.base1SubString(queryNts, queryNtStart, queryNtEnd);

					String queryAAs = segAas.substring(matcherStart, matcherEnd);
					String firstRefCodon = firstLabeledCodon.getCodonLabel();
					String lastRefCodon = lastLabeledCodon.getCodonLabel();
					AminoAcidRegexPolymorphismMatchResult aapmr = 
							new AminoAcidRegexPolymorphismMatchResult(firstRefCodon, lastRefCodon, 
									refNtStart, refNtEnd, 
									queryNtStart, queryNtEnd, queryAAs, polymorphismQueryNts);
					if(qualityString != null) {
						aapmr.setWorstContributingQScore(SamUtils.worstQScore(qualityString, queryNtStart, queryNtEnd));
					}
					matchResults.add(aapmr);
				}
			}
		}
		VariationScanResult<AminoAcidRegexPolymorphismMatchResult> variationScanResult = new VariationScanResult<AminoAcidRegexPolymorphismMatchResult>(this, sufficientCoverage, matchResults);
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
