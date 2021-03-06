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

import gnu.trove.map.TIntObjectMap;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationMetatag.VariationMetatag.VariationMetatagType;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;


public class AminoAcidDeletionScanner extends BaseAminoAcidVariationScanner<AminoAcidDeletionMatchResult> {

	private static final List<VariationMetatagType> allowedMetatagTypes = 
			Arrays.asList(VariationMetatagType.FLANKING_AAS, 
							VariationMetatagType.ALLOW_PARTIAL_COVERAGE,
							VariationMetatagType.MIN_DELETION_LENGTH_AAS,
							VariationMetatagType.MAX_DELETION_LENGTH_AAS);
	private static final List<VariationMetatagType> requiredMetatagTypes = Arrays.asList();

	private int flankingAas;
	private Integer minDeletionLengthAas;
	private Integer maxDeletionLengthAas;
	private Boolean allowPartialCoverage;
	private String referenceNucleotides;
	private TIntObjectMap<LabeledCodon> startRefNtToLabeledCodon;
	private TIntObjectMap<LabeledCodon> endRefNtToLabeledCodon;
	
	
	public AminoAcidDeletionScanner() {
		super(allowedMetatagTypes, requiredMetatagTypes);
	}

	@Override
	public void init(CommandContext cmdContext) {
		super.init(cmdContext);
		FeatureLocation featureLoc = getVariation().getFeatureLoc();
		this.referenceNucleotides = featureLoc.getReferenceSequence()
				.getSequence().getSequenceObject().getNucleotides(cmdContext);
		this.startRefNtToLabeledCodon = featureLoc.getStartRefNtToLabeledCodon(cmdContext);
		this.endRefNtToLabeledCodon = featureLoc.getEndRefNtToLabeledCodon(cmdContext);
		Integer configuredFlankingAas = getIntMetatagValue(VariationMetatagType.FLANKING_AAS);
		if(configuredFlankingAas != null) {
			this.flankingAas = configuredFlankingAas;
		} else {
			this.flankingAas = 1;
		}
		Integer configuredMinDeletionLengthAas = getIntMetatagValue(VariationMetatagType.MIN_DELETION_LENGTH_AAS);
		if(configuredMinDeletionLengthAas != null) {
			this.minDeletionLengthAas = configuredMinDeletionLengthAas;
		} else {
			this.minDeletionLengthAas = null;
		}
		Integer configuredMaxDeletionLengthAas = getIntMetatagValue(VariationMetatagType.MAX_DELETION_LENGTH_AAS);
		if(configuredMaxDeletionLengthAas != null) {
			this.maxDeletionLengthAas = configuredMaxDeletionLengthAas;
		} else {
			this.maxDeletionLengthAas = null;
		}
		Boolean configuredAllowPartialCoverage = getBooleanMetatagValue(VariationMetatagType.ALLOW_PARTIAL_COVERAGE);
		if(configuredAllowPartialCoverage != null) {
			this.allowPartialCoverage = configuredAllowPartialCoverage;
		} else {
			this.allowPartialCoverage = false;
		}
	}
	
	@Override
	public List<ReferenceSegment> getSegmentsToCover() {
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
	protected boolean computeSufficientCoverage(List<QueryAlignedSegment> queryToRefSegs) {
		if(this.allowPartialCoverage) {
			return true;
		}
		return super.computeSufficientCoverage(queryToRefSegs);
	}

	@Override
	protected VariationScanResult<AminoAcidDeletionMatchResult> scanInternal(
			CommandContext cmdContext, 
			List<QueryAlignedSegment> queryToRefSegs, String queryNts, String qualityString) {
		List<AminoAcidDeletionMatchResult> matchResults = new ArrayList<AminoAcidDeletionMatchResult>();
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
					int qryLastNtBeforeDel = lastSegment.getQueryEnd();
					int qryFirstNtAfterDel = currentSegment.getQueryStart();
					// query segments are abutting
					if(qryLastNtBeforeDel == (qryFirstNtAfterDel-1)) {
						int refFirstNtDeleted = lastSegment.getRefEnd() + 1;
						int refLastNtDeleted = currentSegment.getRefStart() - 1;
						// some reference nucleotides were deleted.
						if(refFirstNtDeleted <= refLastNtDeleted) {
							// the length of both segments is at least the flankingNTs
							if(lastSegment.getCurrentLength() >= (this.flankingAas * 3) && 
									currentSegment.getCurrentLength() >= (this.flankingAas * 3)) {
								// deleted region is within configured min / max lengths
								int deletedRegionLength = (refLastNtDeleted - refFirstNtDeleted) + 1;
								if( (minDeletionLengthAas == null || deletedRegionLength >= (minDeletionLengthAas*3)) && 
										(maxDeletionLengthAas == null || deletedRegionLength <= (maxDeletionLengthAas*3)) ) {
									String deletedRefNts = FastaUtils.subSequence(referenceNucleotides, refFirstNtDeleted, refLastNtDeleted).toString();
									boolean deletionIsCodonAligned = false;
									String refFirstCodonDeleted = null;
									String refLastCodonDeleted = null;
									String deletedRefAas = null;
									if(startRefNtToLabeledCodon.get(refFirstNtDeleted) != null &&
											endRefNtToLabeledCodon.get(refLastNtDeleted) != null) {
										deletionIsCodonAligned = true;
										refFirstCodonDeleted = startRefNtToLabeledCodon.get(refFirstNtDeleted).getCodonLabel();
										refLastCodonDeleted = startRefNtToLabeledCodon.get(refLastNtDeleted-2).getCodonLabel();
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
		return new VariationScanResult<AminoAcidDeletionMatchResult>(this, sufficientCoverage, matchResults);
	}


}
