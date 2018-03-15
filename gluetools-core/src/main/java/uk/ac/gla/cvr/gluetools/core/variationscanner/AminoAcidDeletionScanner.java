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


public class AminoAcidDeletionScanner extends BaseAminoAcidVariationScanner<AminoAcidDeletionMatchResult> {

	private static final List<VariationMetatagType> allowedMetatagTypes = 
			Arrays.asList(VariationMetatagType.FLANKING_AAS, 
							VariationMetatagType.FLANKING_NTS, 
							VariationMetatagType.MIN_DELETION_LENGTH_AAS,
							VariationMetatagType.MIN_DELETION_LENGTH_NTS,
							VariationMetatagType.MAX_DELETION_LENGTH_AAS,
							VariationMetatagType.MAX_DELETION_LENGTH_NTS);
	private static final List<VariationMetatagType> requiredMetatagTypes = Arrays.asList();

	private int flankingNTs;
	private Integer minDeletionLengthNts;
	private Integer maxDeletionLengthNts;
	private String referenceNucleotides;
	private int codon1Start;
	private TIntObjectMap<LabeledCodon> refNtToLabeledCodon;
	
	
	public AminoAcidDeletionScanner() {
		super(allowedMetatagTypes, requiredMetatagTypes);
	}

	@Override
	public void validate() {
		super.validate();
		Map<VariationMetatagType, String> metatagsMap = getMetatagsMap();
		if(metatagsMap.containsKey(VariationMetatagType.FLANKING_AAS) &&
				metatagsMap.containsKey(VariationMetatagType.FLANKING_NTS)) {
			throwScannerException("Only one of FLANKING_AAS and FLANKING_NTS may be defined");
		}
	}
	
	@Override
	public void init(CommandContext cmdContext) {
		FeatureLocation featureLoc = getVariation().getFeatureLoc();
		this.referenceNucleotides = featureLoc.getReferenceSequence()
				.getSequence().getSequenceObject().getNucleotides(cmdContext);
		this.codon1Start = featureLoc.getCodon1Start(cmdContext);
		this.refNtToLabeledCodon = featureLoc.getRefNtToLabeledCodon(cmdContext);
		Integer configuredFlankingAas = getIntMetatagValue(VariationMetatagType.FLANKING_AAS);
		Integer configuredFlankingNts = getIntMetatagValue(VariationMetatagType.FLANKING_NTS);
		if(configuredFlankingAas != null && configuredFlankingNts != null) {
			throwScannerException("Only one of FLANKING_AAS and FLANKING_NTS may be defined");
		}
		if(configuredFlankingNts != null) {
			this.flankingNTs = configuredFlankingNts;
		} else if(configuredFlankingAas != null) {
			this.flankingNTs = configuredFlankingAas*3;
		} else {
			this.flankingNTs = 3;
		}
		Integer configuredMinDeletionLengthAas = getIntMetatagValue(VariationMetatagType.MIN_DELETION_LENGTH_AAS);
		Integer configuredMinDeletionLengthNts = getIntMetatagValue(VariationMetatagType.MIN_DELETION_LENGTH_NTS);
		if(configuredMinDeletionLengthAas != null && configuredMinDeletionLengthNts != null) {
			throwScannerException("Only one of MIN_DELETION_LENGTH_AAS and MIN_DELETION_LENGTH_NTS may be defined");
		}
		if(configuredMinDeletionLengthNts != null) {
			this.minDeletionLengthNts = configuredMinDeletionLengthNts;
		} else if(configuredFlankingAas != null) {
			this.minDeletionLengthNts = configuredMinDeletionLengthAas*3;
		} else {
			this.minDeletionLengthNts = null;
		}
		Integer configuredMaxDeletionLengthAas = getIntMetatagValue(VariationMetatagType.MAX_DELETION_LENGTH_AAS);
		Integer configuredMaxDeletionLengthNts = getIntMetatagValue(VariationMetatagType.MAX_DELETION_LENGTH_NTS);
		if(configuredMaxDeletionLengthAas != null && configuredMaxDeletionLengthNts != null) {
			throwScannerException("Only one of MAX_DELETION_LENGTH_AAS and MAX_DELETION_LENGTH_NTS may be defined");
		}
		if(configuredMaxDeletionLengthNts != null) {
			this.maxDeletionLengthNts = configuredMaxDeletionLengthNts;
		} else if(configuredFlankingAas != null) {
			this.maxDeletionLengthNts = configuredMaxDeletionLengthAas*3;
		} else {
			this.maxDeletionLengthNts = null;
		}
	}
	
	@Override
	protected boolean computeSufficientCoverage(List<NtQueryAlignedSegment> queryToRefNtSegs) {
		Integer flankingStart = computeFlankingStart();
		Integer flankingEnd = computeFlankingEnd();
		Integer refStart = getVariation().getRefStart();
		Integer refEnd = getVariation().getRefEnd();
		return ReferenceSegment.covers(queryToRefNtSegs, 
				Arrays.asList(new ReferenceSegment(flankingStart, refStart-1),
						new ReferenceSegment(refEnd+1, flankingEnd)));
	}

	private Integer computeFlankingStart() {
		return Math.max(getVariation().getRefStart()-this.flankingNTs, 1);
	}
	private Integer computeFlankingEnd() {
		return Math.min(getVariation().getRefEnd()+this.flankingNTs, this.referenceNucleotides.length());
	}



	@Override
	public VariationScanResult<AminoAcidDeletionMatchResult> scan(CommandContext cmdContext, List<NtQueryAlignedSegment> queryToRefNtSegs) {
		List<AminoAcidDeletionMatchResult> matchResults = new ArrayList<AminoAcidDeletionMatchResult>();
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
					int qryLastNtBeforeDel = lastSegment.getQueryEnd();
					int qryFirstNtAfterDel = currentSegment.getQueryStart();
					// query segments are abutting
					if(qryLastNtBeforeDel == (qryFirstNtAfterDel-1)) {
						int refFirstNtDeleted = lastSegment.getRefEnd() + 1;
						int refLastNtDeleted = currentSegment.getRefStart() - 1;
						// some reference nucleotides were deleted.
						if(refFirstNtDeleted <= refLastNtDeleted) {
							// the length of both segments is at least the flankingNTs
							if(lastSegment.getCurrentLength() >= this.flankingNTs && currentSegment.getCurrentLength() >= this.flankingNTs) {
								// deleted region is within configured min / max lengths
								int deletedRegionLength = (refLastNtDeleted - refFirstNtDeleted) + 1;
								if( (minDeletionLengthNts == null || deletedRegionLength >= minDeletionLengthNts) && 
										(maxDeletionLengthNts == null || deletedRegionLength <= maxDeletionLengthNts) ) {
									String deletedRefNts = FastaUtils.subSequence(referenceNucleotides, refFirstNtDeleted, refLastNtDeleted).toString();
									boolean deletionIsCodonAligned = false;
									String refFirstCodonDeleted = null;
									String refLastCodonDeleted = null;
									String deletedRefAas = null;
									if(TranslationUtils.isAtStartOfCodon(codon1Start, refFirstNtDeleted) &&
											TranslationUtils.isAtEndOfCodon(codon1Start, refLastNtDeleted)) {
										deletionIsCodonAligned = true;
										refFirstCodonDeleted = refNtToLabeledCodon.get(refFirstNtDeleted).getCodonLabel();
										refLastCodonDeleted = refNtToLabeledCodon.get(refLastNtDeleted-2).getCodonLabel();
										deletedRefAas = TranslationUtils.translateToAaString(deletedRefNts);
									} 
									matchResults.add(new AminoAcidDeletionMatchResult(refFirstCodonDeleted, refLastCodonDeleted, 
											refFirstNtDeleted, refLastNtDeleted, 
											qryLastNtBeforeDel, qryFirstNtAfterDel, 
											deletedRefNts, deletedRefAas, deletionIsCodonAligned));
								}
							}
						}
					}
				}
				lastSegment = currentSegment;
			}
		}
		return new VariationScanResult<AminoAcidDeletionMatchResult>(getVariation(), sufficientCoverage, matchResults);
	}


}
