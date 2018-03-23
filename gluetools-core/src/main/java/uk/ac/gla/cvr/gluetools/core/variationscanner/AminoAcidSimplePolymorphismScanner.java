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

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationMetatag.VariationMetatag.VariationMetatagType;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.translation.AmbigNtTripletInfo;
import uk.ac.gla.cvr.gluetools.core.translation.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;

public class AminoAcidSimplePolymorphismScanner extends BaseAminoAcidVariationScanner<AminoAcidSimplePolymorphismMatchResult> {

	private static final List<VariationMetatagType> allowedMetatagTypes = 
			Arrays.asList(VariationMetatagType.SIMPLE_AA_PATTERN, 
							VariationMetatagType.MIN_COVERAGE_NTS, 
							VariationMetatagType.MIN_COMBINED_TRIPLET_FRACTION);
	private static final List<VariationMetatagType> requiredMetatagTypes = 
			Arrays.asList(VariationMetatagType.SIMPLE_AA_PATTERN);

	private String simpleAaPattern;
	
	// metatag for dealing with ambiguous nucleotide bases.
	// for each aa residue in the simple pattern, a triplet of three
	// possibly ambiguous nucleotide characters is scanned. 
	// the fraction of underlying consistent triplets which code for the
	// pattern aa is the triplet fraction. If the pattern contains 
	// multiple residues, the triplet fractions of each are multiplied together.
	// The combinedTripletFraction is part of the AminoAcidSimplePolymorphismMatchResult
	// if this is greater than minCombinedTripletFraction, it is considered a match. 
	// Default is 1.0 (no ambiguity).
	private Double minCombinedTripletFraction;

	public AminoAcidSimplePolymorphismScanner() {
		super(allowedMetatagTypes, requiredMetatagTypes);
	}
	
	@Override
	protected void init(CommandContext cmdContext) {
		super.init(cmdContext);		
		this.simpleAaPattern = getStringMetatagValue(VariationMetatagType.SIMPLE_AA_PATTERN);
		this.minCombinedTripletFraction = getDoubleMetatagValue(VariationMetatagType.MIN_COMBINED_TRIPLET_FRACTION);
		if(this.minCombinedTripletFraction == null) {
			this.minCombinedTripletFraction = 1.0;
		}
	}



	@Override
	protected VariationScanResult<AminoAcidSimplePolymorphismMatchResult> scanInternal(
			CommandContext cmdContext,
			List<NtQueryAlignedSegment> queryToRefNtSegs, String queryNts) {
		List<AminoAcidSimplePolymorphismMatchResult> matchResults = new ArrayList<AminoAcidSimplePolymorphismMatchResult>();
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

			for(NtQueryAlignedSegment ntQaSeg: ntQaSegsCdnAligned) {
				String segNts = ntQaSeg.getNucleotides().toString();
				List<AmbigNtTripletInfo> ambigTripletInfos = translator.translate(segNts);
				TripletInfosMatch tripletInfosMatch;
				int nextIndex = 0;
				do {
					tripletInfosMatch = tripletInfosMatch(ambigTripletInfos, nextIndex, simpleAaPattern, minCombinedTripletFraction);
					if(tripletInfosMatch != null) {
						int queryNtStart = ntQaSeg.getQueryStart() + (tripletInfosMatch.index*3);
						int queryNtEnd = queryNtStart + (((simpleAaPattern.length()-1)*3)+2);
						int refNtStart = queryNtStart + ntQaSeg.getQueryToReferenceOffset();
						int refNtEnd = queryNtEnd + ntQaSeg.getQueryToReferenceOffset();
						String polymorphismQueryNts = segNts.substring(
								queryNtStart - ntQaSeg.getQueryStart(), 
								(queryNtEnd - ntQaSeg.getQueryStart())+1);
						String queryAAs = tripletInfosMatch.queryAas.toString();
						String firstRefCodon = refNtToLabeledCodon.get(refNtStart).getCodonLabel();
						String lastRefCodon = refNtToLabeledCodon.get(refNtEnd-2).getCodonLabel();
						AminoAcidSimplePolymorphismMatchResult aaspmr = 
								new AminoAcidSimplePolymorphismMatchResult(firstRefCodon, lastRefCodon, 
										refNtStart, refNtEnd, 
										queryNtStart, queryNtEnd, queryAAs, polymorphismQueryNts, tripletInfosMatch.combinedTripletFraction);
						matchResults.add(aaspmr);
						nextIndex = tripletInfosMatch.index+1;
					}
				} while(tripletInfosMatch != null);
			}
		}
		return new VariationScanResult<AminoAcidSimplePolymorphismMatchResult>(getVariation(), sufficientCoverage, matchResults);
	}


	private static class TripletInfosMatch {
		int index;
		StringBuffer queryAas = new StringBuffer();
		double combinedTripletFraction = 1.0;
	}

	private TripletInfosMatch tripletInfosMatch(List<AmbigNtTripletInfo> ambigTripletInfos, int fromIndex, String pattern, double minCombinedTripletFraction) {
		for(int startIndex = fromIndex; startIndex < (ambigTripletInfos.size() - pattern.length()) + 1; startIndex++) {
			TripletInfosMatch tripletInfosMatch = new TripletInfosMatch();
			tripletInfosMatch.index = startIndex;
			boolean match = true;
			for(int i = 0; i < pattern.length(); i++) {
				char aa = pattern.charAt(i);
				AmbigNtTripletInfo ambigNtTripletInfo = ambigTripletInfos.get(startIndex+i);
				double aaTripletsFraction = ambigNtTripletInfo.getPossibleAaTripletsFraction(aa);
				tripletInfosMatch.combinedTripletFraction *= aaTripletsFraction;
				tripletInfosMatch.queryAas.append(ambigNtTripletInfo.getSingleCharTranslation());
				if(tripletInfosMatch.combinedTripletFraction < minCombinedTripletFraction) {
					match = false;
					break;
				}
			}
			if(match) {
				return tripletInfosMatch;
			}
		}
		return null;
	}
	
	
}
