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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationMetatag.VariationMetatag.VariationMetatagType;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.translation.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;

public class AminoAcidPolymorphismScanner extends BaseAminoAcidVariationScanner<AminoAcidPolymorphismMatchResult> {

	private static final List<VariationMetatagType> allowedMetatagTypes = 
			Arrays.asList(VariationMetatagType.SIMPLE_AA_PATTERN, 
							VariationMetatagType.REGEX_AA_PATTERN);
	private static final List<VariationMetatagType> requiredMetatagTypes = Arrays.asList();

	public AminoAcidPolymorphismScanner() {
		super(allowedMetatagTypes, requiredMetatagTypes);
	}

	
	
	@Override
	public void validate() {
		super.validate();
		Map<VariationMetatagType, String> metatagsMap = getMetatagsMap();
		if(metatagsMap.containsKey(VariationMetatagType.SIMPLE_AA_PATTERN) &&
				metatagsMap.containsKey(VariationMetatagType.REGEX_AA_PATTERN)) {
			throwScannerException("Only one of SIMPLE_AA_PATTERN and REGEX_AA_PATTERN may be defined");
		}
		if((!metatagsMap.containsKey(VariationMetatagType.SIMPLE_AA_PATTERN)) &&
				!metatagsMap.containsKey(VariationMetatagType.REGEX_AA_PATTERN)) {
			throwScannerException("At least one of SIMPLE_AA_PATTERN and REGEX_AA_PATTERN must be defined");
		}
	}

	@Override
	public VariationScanResult<AminoAcidPolymorphismMatchResult> scan(
			CommandContext cmdContext,
			List<NtQueryAlignedSegment> queryToRefNtSegs) {
		List<AminoAcidPolymorphismMatchResult> matchResults = new ArrayList<AminoAcidPolymorphismMatchResult>();
		boolean sufficientCoverage = computeSufficientCoverage(queryToRefNtSegs);
		if(sufficientCoverage) {
			Translator translator = new CommandContextTranslator(cmdContext);
			Variation variation = getVariation();
			FeatureLocation featureLoc = variation.getFeatureLoc();
			TIntObjectMap<LabeledCodon> refNtToLabeledCodon = featureLoc.getRefNtToLabeledCodon(cmdContext);
			Integer codon1Start = featureLoc.getCodon1Start(cmdContext);

			List<NtQueryAlignedSegment> queryToRefNtSegsVariationRegion = 
					ReferenceSegment.intersection(queryToRefNtSegs, Arrays.asList(new ReferenceSegment(variation.getRefStart(), variation.getRefEnd())), 
							ReferenceSegment.cloneLeftSegMerger());

			List<NtQueryAlignedSegment> ntQaSegsCdnAligned = TranslationUtils.truncateToCodonAligned(codon1Start, queryToRefNtSegsVariationRegion);

			Map<VariationMetatagType, String> metatagsMap = getMetatagsMap();
			String simpleAaPattern = metatagsMap.get(VariationMetatagType.SIMPLE_AA_PATTERN);
			String regexAaPattern = metatagsMap.get(VariationMetatagType.REGEX_AA_PATTERN);
			if(simpleAaPattern != null) {
				for(NtQueryAlignedSegment ntQaSeg: ntQaSegsCdnAligned) {
					String segNts = ntQaSeg.getNucleotides().toString();
					String segAas = translator.translateToAaString(segNts);
					int nextIndex = -1;
					do {
						nextIndex = segAas.indexOf(simpleAaPattern, nextIndex+1);
						if(nextIndex >= 0) {
							int queryNtStart = ntQaSeg.getQueryStart() + (nextIndex*3);
							int queryNtEnd = queryNtStart + (((simpleAaPattern.length()-1)*3)+2);
							int refNtStart = queryNtStart + ntQaSeg.getQueryToReferenceOffset();
							int refNtEnd = queryNtEnd + ntQaSeg.getQueryToReferenceOffset();
							String queryNts = segNts.substring(
									queryNtStart - ntQaSeg.getQueryStart(), 
									(queryNtEnd - ntQaSeg.getQueryStart())+1);
							String queryAAs = segAas.substring(nextIndex, nextIndex+simpleAaPattern.length());
							String firstRefCodon = refNtToLabeledCodon.get(refNtStart).getCodonLabel();
							String lastRefCodon = refNtToLabeledCodon.get(refNtEnd-2).getCodonLabel();
							AminoAcidPolymorphismMatchResult aapmr = 
									new AminoAcidPolymorphismMatchResult(firstRefCodon, lastRefCodon, 
											refNtStart, refNtEnd, 
											queryNtStart, queryNtEnd, queryAAs, queryNts);
							matchResults.add(aapmr);
						}
					} while(nextIndex != -1);
				}
			} else if(regexAaPattern != null) {
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
						String queryNts = segNts.substring(
								queryNtStart - ntQaSeg.getQueryStart(), 
								(queryNtEnd - ntQaSeg.getQueryStart())+1);
						String queryAAs = segAas.substring(matcherStart, matcherEnd);
						String firstRefCodon = refNtToLabeledCodon.get(refNtStart).getCodonLabel();
						String lastRefCodon = refNtToLabeledCodon.get(refNtEnd-2).getCodonLabel();
						AminoAcidPolymorphismMatchResult aapmr = 
								new AminoAcidPolymorphismMatchResult(firstRefCodon, lastRefCodon, 
										refNtStart, refNtEnd, 
										queryNtStart, queryNtEnd, queryAAs, queryNts);
						matchResults.add(aapmr);
					}
				}
			} else {
				throwScannerException("Neither SIMPLE_AA_PATTERN nor REGEX_AA_PATTERN metatags are defined");
			}
		}
		return new VariationScanResult<AminoAcidPolymorphismMatchResult>(getVariation(), sufficientCoverage, matchResults);
	}


	
	
}
