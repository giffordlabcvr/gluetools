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

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationMetatag.VariationMetatag.VariationMetatagType;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

public class NucleotideDeletionScanner extends BaseNucleotideVariationScanner<NucleotideDeletionMatchResult> {

	private static final List<VariationMetatagType> allowedMetatagTypes = Arrays.asList(
			VariationMetatagType.FLANKING_NTS, 
			VariationMetatagType.ALLOW_PARTIAL_COVERAGE,
			VariationMetatagType.MIN_DELETION_LENGTH_NTS,
			VariationMetatagType.MAX_DELETION_LENGTH_NTS);
	private static final List<VariationMetatagType> requiredMetatagTypes = Arrays.asList();

	private int flankingNTs;
	private String referenceNucleotides;
	private Integer minDeletionLengthNts;
	private Integer maxDeletionLengthNts;
	private Boolean allowPartialCoverage;
	
	public NucleotideDeletionScanner() {
		super(allowedMetatagTypes, requiredMetatagTypes);
	}

	@Override
	public void init(CommandContext cmdContext) {
		super.init(cmdContext);
		FeatureLocation featureLoc = getVariation().getFeatureLoc();
		this.referenceNucleotides = featureLoc.getReferenceSequence()
				.getSequence().getSequenceObject().getNucleotides(cmdContext);
		Integer configuredFlankingNts = getIntMetatagValue(VariationMetatagType.FLANKING_NTS);
		if(configuredFlankingNts != null) {
			this.flankingNTs = configuredFlankingNts;
		} else {
			this.flankingNTs = 3;
		}
		Integer configuredMinDeletionLengthNts = getIntMetatagValue(VariationMetatagType.MIN_DELETION_LENGTH_NTS);
		if(configuredMinDeletionLengthNts != null) {
			this.minDeletionLengthNts = configuredMinDeletionLengthNts;
		} else {
			this.minDeletionLengthNts = null;
		}
		Integer configuredMaxDeletionLengthNts = getIntMetatagValue(VariationMetatagType.MAX_DELETION_LENGTH_NTS);
		if(configuredMaxDeletionLengthNts != null) {
			this.maxDeletionLengthNts = configuredMaxDeletionLengthNts;
		} else {
			this.maxDeletionLengthNts = null;
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
		return Math.max(getVariation().getRefStart()-this.flankingNTs, 1);
	}
	private Integer computeFlankingEnd() {
		return Math.min(getVariation().getRefEnd()+this.flankingNTs, this.referenceNucleotides.length());
	}


	
	@Override
	protected boolean computeSufficientCoverage(List<QueryAlignedSegment> queryToRefSegs) {
		if(this.allowPartialCoverage) {
			return true;
		}
		return super.computeSufficientCoverage(queryToRefSegs);
	}

	@Override
	protected VariationScanResult<NucleotideDeletionMatchResult> scanInternal(
			CommandContext cmdContext, 
			List<QueryAlignedSegment> queryToRefSegs, String queryNts, String qualityString) {
		List<NucleotideDeletionMatchResult> matchResults = new ArrayList<NucleotideDeletionMatchResult>();
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
							if(lastSegment.getCurrentLength() >= this.flankingNTs && currentSegment.getCurrentLength() >= this.flankingNTs) {
								// deleted region is within configured min / max lengths
								int deletedRegionLength = (refLastNtDeleted - refFirstNtDeleted) + 1;
								if( (minDeletionLengthNts == null || deletedRegionLength >= minDeletionLengthNts) && 
										(maxDeletionLengthNts == null || deletedRegionLength <= maxDeletionLengthNts) ) {
									String deletedRefNts = FastaUtils.subSequence(referenceNucleotides, refFirstNtDeleted, refLastNtDeleted).toString();
									matchResults.add(new NucleotideDeletionMatchResult(
											refFirstNtDeleted, refLastNtDeleted, 
											qryLastNtBeforeDel, qryFirstNtAfterDel, 
											deletedRefNts));
								}
							}
						}
					}
				}
				lastSegment = currentSegment;
			}
		}
		return new VariationScanResult<NucleotideDeletionMatchResult>(this, sufficientCoverage, matchResults);
	}

	

}
