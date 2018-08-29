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
import uk.ac.gla.cvr.gluetools.core.datamodel.variationMetatag.VariationMetatag.VariationMetatagType;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

public class AminoAcidInsertionScanner extends BaseAminoAcidVariationScanner<AminoAcidInsertionMatchResult> {

	private static final List<VariationMetatagType> allowedMetatagTypes = 
			Arrays.asList(VariationMetatagType.FLANKING_AAS, 
							VariationMetatagType.MIN_INSERTION_LENGTH_AAS,
							VariationMetatagType.MAX_INSERTION_LENGTH_AAS);
	private static final List<VariationMetatagType> requiredMetatagTypes = Arrays.asList();

	private int flankingAas;
	private Integer minInsertionLengthAas;
	private Integer maxInsertionLengthAas;
	private String referenceNucleotides;
	private int codon1Start;
	private TIntObjectMap<LabeledCodon> refNtToLabeledCodon;
	
	
	public AminoAcidInsertionScanner() {
		super(allowedMetatagTypes, requiredMetatagTypes);
	}

	@Override
	public void init(CommandContext cmdContext) {
		super.init(cmdContext);
		FeatureLocation featureLoc = getVariation().getFeatureLoc();
		this.referenceNucleotides = featureLoc.getReferenceSequence()
				.getSequence().getSequenceObject().getNucleotides(cmdContext);
		this.codon1Start = featureLoc.getCodon1Start(cmdContext);
		this.refNtToLabeledCodon = featureLoc.getRefNtToLabeledCodon(cmdContext);
		Integer configuredFlankingAas = getIntMetatagValue(VariationMetatagType.FLANKING_AAS);
		if(configuredFlankingAas != null) {
			this.flankingAas = configuredFlankingAas;
		} else {
			this.flankingAas = 1;
		}
		Integer configuredMinInsertionLengthAas = getIntMetatagValue(VariationMetatagType.MIN_INSERTION_LENGTH_AAS);
		if(configuredMinInsertionLengthAas != null) {
			this.minInsertionLengthAas = configuredMinInsertionLengthAas;
		} else {
			this.minInsertionLengthAas = null;
		}
		Integer configuredMaxInsertionLengthAas = getIntMetatagValue(VariationMetatagType.MAX_INSERTION_LENGTH_AAS);
		if(configuredMaxInsertionLengthAas != null) {
			this.maxInsertionLengthAas = configuredMaxInsertionLengthAas;
		} else {
			this.maxInsertionLengthAas = null;
		}
	}
	
	@Override
	public List<ReferenceSegment> getSegmentsToCover() {
		Integer flankingStart = computeFlankingStart();
		Integer flankingEnd = computeFlankingEnd();
		Integer refStart = getVariation().getRefStart();
		Integer refEnd = getVariation().getRefEnd();
		return Arrays.asList(new ReferenceSegment(flankingStart, refStart),
						new ReferenceSegment(refEnd, flankingEnd));
	}

	private Integer computeFlankingStart() {
		Integer refStart = getVariation().getRefStart();
		return Math.max((refStart+1)-(this.flankingAas*3), 1);
	}
	private Integer computeFlankingEnd() {
		Integer refEnd = getVariation().getRefEnd();
		return Math.min((refEnd-1)+(this.flankingAas*3), this.referenceNucleotides.length());
	}

	// this variation scanner picks up insertions strictly *between* the start and end NTs
	// Example: Assume codon labels 220 and 221 are adjacent opn the reference. 
	// If the variation is defined by --labeledCodon 220 221, then the query sequence
	// must have segments homologous to 220 and to 221 on the reference, and have some insertion between
	// these segments.
	
	@Override
	protected VariationScanResult<AminoAcidInsertionMatchResult> scanInternal(
			CommandContext cmdContext, 
			List<QueryAlignedSegment> queryToRefSegs, String queryNts, String qualityString) {
		List<AminoAcidInsertionMatchResult> matchResults = new ArrayList<AminoAcidInsertionMatchResult>();
		boolean sufficientCoverage = computeSufficientCoverage(queryToRefSegs);
		if(sufficientCoverage) {
			Integer flankingStart = computeFlankingStart();
			Integer flankingEnd = computeFlankingEnd();

			List<QueryAlignedSegment> queryToRefSegsTrimmed = 
					ReferenceSegment.intersection(queryToRefSegs,
							Arrays.asList(new ReferenceSegment(flankingStart, flankingEnd)), ReferenceSegment.cloneLeftSegMerger());
			
			QueryAlignedSegment lastSegment = null;
			
			for(QueryAlignedSegment currentSegment: queryToRefSegsTrimmed) {
				if(lastSegment != null) {
					int refLastNtBeforeIns = lastSegment.getRefEnd();
					int refFirstNtAfterIns = currentSegment.getRefStart();
					// reference segments are abutting
					if(refLastNtBeforeIns == (refFirstNtAfterIns-1)) {
						int qryFirstInsertedNt = lastSegment.getQueryEnd() + 1;
						int qryLastInsertedNt = currentSegment.getQueryStart() - 1;
						// some reference nucleotides were deleted.
						if(qryFirstInsertedNt <= qryLastInsertedNt) {
							// the length of both segments is at least the flankingNTs
							if(lastSegment.getCurrentLength() >= (this.flankingAas * 3) && 
									currentSegment.getCurrentLength() >= (this.flankingAas * 3)) {
								// inserted region is within configured min / max lengths
								int insertedRegionLength = (qryLastInsertedNt - qryFirstInsertedNt) + 1;
								if( (minInsertionLengthAas == null || insertedRegionLength >= (minInsertionLengthAas*3)) && 
										(maxInsertionLengthAas == null || insertedRegionLength <= (maxInsertionLengthAas*3)) ) {
									String insertedQryNts = FastaUtils.subSequence(queryNts, qryFirstInsertedNt, qryLastInsertedNt).toString();
									boolean insertionIsCodonAligned = false;
									String refLastCodonBeforeIns = null;
									String refFirstCodonAfterIns = null;
									String insertedQryAas = null;
									if(TranslationUtils.isAtEndOfCodon(codon1Start, refLastNtBeforeIns) &&
											TranslationUtils.isAtStartOfCodon(codon1Start, refFirstNtAfterIns) &&
											insertedRegionLength % 3 == 0) {
										insertionIsCodonAligned = true;
										refLastCodonBeforeIns = refNtToLabeledCodon.get(refLastNtBeforeIns-2).getCodonLabel();
										refFirstCodonAfterIns = refNtToLabeledCodon.get(refFirstNtAfterIns).getCodonLabel();
										insertedQryAas = TranslationUtils.translateToAaString(insertedQryNts);
									} 
									matchResults.add(new AminoAcidInsertionMatchResult(
											refLastCodonBeforeIns, refFirstCodonAfterIns, 
											refLastNtBeforeIns, refFirstNtAfterIns, 
											qryFirstInsertedNt, qryLastInsertedNt, 
											insertedQryNts, insertedQryAas,
											insertionIsCodonAligned));
								}
							}
						}
					}
				}
				lastSegment = currentSegment;
			}
		}
		return new VariationScanResult<AminoAcidInsertionMatchResult>(this, sufficientCoverage, matchResults);
	}

	@Override
	public void validate() {
		super.validate();
		int ntStart = getVariation().getRefStart();
		int ntEnd = getVariation().getRefEnd();
		
		// ntStart must be in a codon before ntEnd.
		int codonStart = TranslationUtils.getCodon(codon1Start, ntStart);
		int codonEnd = TranslationUtils.getCodon(codon1Start, ntEnd);
		
		if(codonEnd <= codonStart) {
			throwScannerException("For amino acid insertions start codon must be before end codon");
		}
	}


	
}
