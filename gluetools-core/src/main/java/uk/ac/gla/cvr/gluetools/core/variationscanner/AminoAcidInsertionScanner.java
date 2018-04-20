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

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationMetatag.VariationMetatag.VariationMetatagType;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
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
	protected List<ReferenceSegment> getSegmentsToCover() {
		Integer flankingStart = computeFlankingStart();
		Integer flankingEnd = computeFlankingEnd();
		Integer refStart = getVariation().getRefStart();
		Integer refEnd = getVariation().getRefEnd();
		return Arrays.asList(new ReferenceSegment(flankingStart, refStart-1),
						new ReferenceSegment(refEnd+1, flankingEnd));
	}

	private Integer computeFlankingStart() {
		return Math.max(getVariation().getRefStart()-(this.flankingAas*3), 1);
	}
	private Integer computeFlankingEnd() {
		return Math.min(getVariation().getRefEnd()+(this.flankingAas*3), this.referenceNucleotides.length());
	}



	@Override
	protected VariationScanResult<AminoAcidInsertionMatchResult> scanInternal(CommandContext cmdContext, List<NtQueryAlignedSegment> queryToRefNtSegs, String queryNts) {
		List<AminoAcidInsertionMatchResult> matchResults = new ArrayList<AminoAcidInsertionMatchResult>();
		boolean sufficientCoverage = computeSufficientCoverage(queryToRefNtSegs);
		if(sufficientCoverage) {
			Integer flankingStart = computeFlankingStart();
			Integer flankingEnd = computeFlankingEnd();

			List<NtQueryAlignedSegment> queryToRefNtSegsTrimmed = 
					ReferenceSegment.intersection(queryToRefNtSegs,
							Arrays.asList(new ReferenceSegment(flankingStart, flankingEnd)), ReferenceSegment.cloneLeftSegMerger());
			
			NtQueryAlignedSegment lastSegment = null;
			
			for(NtQueryAlignedSegment currentSegment: queryToRefNtSegsTrimmed) {
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
		return new VariationScanResult<AminoAcidInsertionMatchResult>(getVariation(), sufficientCoverage, matchResults);
	}


	
}
