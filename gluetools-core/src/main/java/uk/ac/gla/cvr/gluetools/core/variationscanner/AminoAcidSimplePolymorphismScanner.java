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
import uk.ac.gla.cvr.gluetools.core.translation.AmbigNtTripletInfo;
import uk.ac.gla.cvr.gluetools.core.translation.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;

public class AminoAcidSimplePolymorphismScanner extends BaseAminoAcidVariationScanner<AminoAcidSimplePolymorphismMatchResult> {

	private static final List<VariationMetatagType> allowedMetatagTypes = 
			Arrays.asList(VariationMetatagType.SIMPLE_AA_PATTERN, 
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
	private Translator translator;


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
		this.translator = new CommandContextTranslator(cmdContext);
	}



	@Override
	protected VariationScanResult<AminoAcidSimplePolymorphismMatchResult> scanInternal(
			CommandContext cmdContext,
			List<QueryAlignedSegment> queryToRefSegs,
			String queryNts, String qualityString) {
		List<AminoAcidSimplePolymorphismMatchResult> matchResults = new ArrayList<AminoAcidSimplePolymorphismMatchResult>();
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
				TripletInfosMatch tripletInfosMatch;
				int nextIndex = 0;
				do {
					tripletInfosMatch = tripletInfosMatch(contiguousLqaaSection, nextIndex, simpleAaPattern, minCombinedTripletFraction);
					if(tripletInfosMatch != null) {
						LabeledQueryAminoAcid matchStartLqaa = contiguousLqaaSection.get(tripletInfosMatch.index);
						LabeledQueryAminoAcid matchEndLqaa = contiguousLqaaSection.get(tripletInfosMatch.index+simpleAaPattern.length()-1);
						LabeledCodon firstLabeledCodon = matchStartLqaa.getLabeledAminoAcid().getLabeledCodon();
						LabeledCodon lastLabeledCodon = matchEndLqaa.getLabeledAminoAcid().getLabeledCodon();
						int queryNtStart = matchStartLqaa.getQueryNtStart();
						int queryNtEnd = matchEndLqaa.getQueryNtEnd();
						int refNtStart = firstLabeledCodon.getNtStart();
						int refNtEnd = lastLabeledCodon.getNtEnd();
						String polymorphismQueryNts = SegmentUtils.base1SubString(queryNts, queryNtStart, queryNtEnd);
						String queryAAs = tripletInfosMatch.queryAas.toString();
						String firstRefCodon = firstLabeledCodon.getCodonLabel();
						String lastRefCodon = lastLabeledCodon.getCodonLabel();
						AminoAcidSimplePolymorphismMatchResult aaspmr = 
								new AminoAcidSimplePolymorphismMatchResult(firstRefCodon, lastRefCodon, 
										refNtStart, refNtEnd, 
										queryNtStart, queryNtEnd, queryAAs, polymorphismQueryNts, 
										tripletInfosMatch.combinedTripletFraction, tripletInfosMatch.reliesOnNonDefiniteAa);
						if(qualityString != null) {
							aaspmr.setWorstContributingQScore(SamUtils.worstQScore(qualityString, queryNtStart, queryNtEnd));
						}
						matchResults.add(aaspmr);
						nextIndex = tripletInfosMatch.index+1;
					}
				} while(tripletInfosMatch != null);
			}
		}
		VariationScanResult<AminoAcidSimplePolymorphismMatchResult> variationScanResult = 
				new VariationScanResult<AminoAcidSimplePolymorphismMatchResult>(this, sufficientCoverage, matchResults);
		if(sufficientCoverage && qualityString != null) {
			if(matchResults.isEmpty()) {
				variationScanResult.setQScore(worstQScoreOfSegments(qualityString, queryToRefSegsVariationRegion));
			} else {
				variationScanResult.setQScore(bestQScoreOfMatchResults(matchResults));
			}
		}
		return variationScanResult;
	}

	private static class TripletInfosMatch {
		int index;
		StringBuffer queryAas = new StringBuffer();
		double combinedTripletFraction = 1.0;
		// if this is set to true, the match relies on an AA translation that 
		// is possible (consistent with the ambiguous nucleotides), but not definite.
		boolean reliesOnNonDefiniteAa = false;
	}

	private TripletInfosMatch tripletInfosMatch(List<LabeledQueryAminoAcid> contiguousLqaaSection, int fromIndex, String pattern, double minCombinedTripletFraction) {
		for(int startIndex = fromIndex; startIndex < (contiguousLqaaSection.size() - pattern.length()) + 1; startIndex++) {
			TripletInfosMatch tripletInfosMatch = new TripletInfosMatch();
			tripletInfosMatch.index = startIndex;
			boolean match = true;
			for(int i = 0; i < pattern.length(); i++) {
				char aa = pattern.charAt(i);
				AmbigNtTripletInfo ambigNtTripletInfo = contiguousLqaaSection.get(startIndex+i).getLabeledAminoAcid().getTranslationInfo();
				double aaTripletsFraction = ambigNtTripletInfo.getPossibleAaTripletsFraction(aa);
				tripletInfosMatch.combinedTripletFraction *= aaTripletsFraction;
				tripletInfosMatch.queryAas.append(ambigNtTripletInfo.getSingleCharTranslation());
				if(tripletInfosMatch.combinedTripletFraction < minCombinedTripletFraction) {
					match = false;
					break;
				}
				if(!ambigNtTripletInfo.getDefiniteAminoAcids().contains(aa)) {
					tripletInfosMatch.reliesOnNonDefiniteAa = true;
				}
			}
			if(match) {
				return tripletInfosMatch;
			}
		}
		return null;
	}
	
	
}
