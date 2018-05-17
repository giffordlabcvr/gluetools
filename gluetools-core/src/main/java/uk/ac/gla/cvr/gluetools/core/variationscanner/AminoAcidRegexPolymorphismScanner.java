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

import gnu.trove.map.TIntObjectMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationMetatag.VariationMetatag.VariationMetatagType;
import uk.ac.gla.cvr.gluetools.core.reporting.samReporter.SamUtils;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.translation.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;

public class AminoAcidRegexPolymorphismScanner extends BaseAminoAcidVariationScanner<AminoAcidRegexPolymorphismMatchResult> {

	private static final List<VariationMetatagType> allowedMetatagTypes = 
			Arrays.asList(VariationMetatagType.REGEX_AA_PATTERN);
	private static final List<VariationMetatagType> requiredMetatagTypes = Arrays.asList(VariationMetatagType.REGEX_AA_PATTERN);

	private String regexAaPattern;
	private Translator translator;
	private TIntObjectMap<LabeledCodon> refNtToLabeledCodon;
	private Integer codon1Start;

	public AminoAcidRegexPolymorphismScanner() {
		super(allowedMetatagTypes, requiredMetatagTypes);
	}

	
	
	@Override
	protected void init(CommandContext cmdContext) {
		super.init(cmdContext);		
		this.regexAaPattern = getStringMetatagValue(VariationMetatagType.REGEX_AA_PATTERN);
		this.translator = new CommandContextTranslator(cmdContext);
		Variation variation = getVariation();
		FeatureLocation featureLoc = variation.getFeatureLoc();
		this.refNtToLabeledCodon = featureLoc.getRefNtToLabeledCodon(cmdContext);
		this.codon1Start = featureLoc.getCodon1Start(cmdContext);

	}

	@Override
	protected VariationScanResult<AminoAcidRegexPolymorphismMatchResult> scanInternal(
			List<NtQueryAlignedSegment> queryToRefNtSegs,
			String queryNts, String qualityString) {
		List<AminoAcidRegexPolymorphismMatchResult> matchResults = new ArrayList<AminoAcidRegexPolymorphismMatchResult>();
		boolean sufficientCoverage = computeSufficientCoverage(queryToRefNtSegs);
		if(sufficientCoverage) {
			Variation variation = getVariation();

			List<NtQueryAlignedSegment> queryToRefNtSegsVariationRegion = 
					ReferenceSegment.intersection(queryToRefNtSegs, Arrays.asList(new ReferenceSegment(variation.getRefStart(), variation.getRefEnd())), 
							ReferenceSegment.cloneLeftSegMerger());

			List<NtQueryAlignedSegment> ntQaSegsCdnAligned = TranslationUtils.truncateToCodonAligned(codon1Start, queryToRefNtSegsVariationRegion);

			Pattern pattern = parseRegex(regexAaPattern);

			for(NtQueryAlignedSegment ntQaSeg: ntQaSegsCdnAligned) {
				String segNts = ntQaSeg.getNucleotides().toString();
				String segAas = translator.translateToAaString(segNts);
				Matcher matcher = pattern.matcher(segAas);
				while(matcher.find()) {
					int matcherStart = matcher.start();
					int matcherEnd = matcher.end();
					int queryNtStart = ntQaSeg.getQueryStart() + (matcherStart*3);
					int queryNtEnd = ntQaSeg.getQueryStart() + (matcherEnd*3) - 1;
					int refNtStart = queryNtStart + ntQaSeg.getQueryToReferenceOffset();
					int refNtEnd = queryNtEnd + ntQaSeg.getQueryToReferenceOffset();
					String polymorphismQueryNts = segNts.substring(
							queryNtStart - ntQaSeg.getQueryStart(), 
							(queryNtEnd - ntQaSeg.getQueryStart())+1);
					String queryAAs = segAas.substring(matcherStart, matcherEnd);
					String firstRefCodon = refNtToLabeledCodon.get(refNtStart).getCodonLabel();
					String lastRefCodon = refNtToLabeledCodon.get(refNtEnd-2).getCodonLabel();
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
		return new VariationScanResult<AminoAcidRegexPolymorphismMatchResult>(getVariation(), sufficientCoverage, matchResults);
	}


	
	
}
