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
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;

public class NucleotideInsertionScanner extends BaseNucleotideVariationScanner<NucleotideInsertionMatchResult> {

	private static final List<VariationMetatagType> allowedMetatagTypes = Arrays.asList(
			VariationMetatagType.FLANKING_NTS, 
			VariationMetatagType.MIN_INSERTION_LENGTH_NTS,
			VariationMetatagType.MAX_INSERTION_LENGTH_NTS);

	private static final List<VariationMetatagType> requiredMetatagTypes = Arrays.asList();

	private int flankingNTs;
	private String referenceNucleotides;
	private Integer minInsertionLengthNts;
	private Integer maxInsertionLengthNts;
	
	public NucleotideInsertionScanner() {
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
		Integer configuredMinDeletionLengthNts = getIntMetatagValue(VariationMetatagType.MIN_INSERTION_LENGTH_NTS);
		if(configuredMinDeletionLengthNts != null) {
			this.minInsertionLengthNts = configuredMinDeletionLengthNts;
		} else {
			this.minInsertionLengthNts = null;
		}
		Integer configuredMaxDeletionLengthNts = getIntMetatagValue(VariationMetatagType.MAX_INSERTION_LENGTH_NTS);
		if(configuredMaxDeletionLengthNts != null) {
			this.maxInsertionLengthNts = configuredMaxDeletionLengthNts;
		} else {
			this.maxInsertionLengthNts = null;
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
		Integer refStart = getVariation().getRefStart();
		return Math.max((refStart+1)-(this.flankingNTs), 1);
	}
	private Integer computeFlankingEnd() {
		Integer refEnd = getVariation().getRefEnd();
		return Math.min((refEnd-1)+(this.flankingNTs), this.referenceNucleotides.length());
	}

	
	// this variation scanner picks up insertions strictly *between* the start and end NTs
	// Example: 
	// If the variation is defined by --nucleotide 6917 6918, then the query sequence
	// must have segments homologous to 6917 6918 on the reference, and have some insertion between
	// these segments.
	
	@Override
	protected VariationScanResult<NucleotideInsertionMatchResult> scanInternal(
			CommandContext cmdContext, 
			List<QueryAlignedSegment> queryToRefSegs, 
			String queryNts, String qualityString) {
		List<NucleotideInsertionMatchResult> matchResults = new ArrayList<NucleotideInsertionMatchResult>();
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
					// ref segments are abutting
					if(refLastNtBeforeIns == (refFirstNtAfterIns-1)) {
						int qryFirstInsertedNt = lastSegment.getQueryEnd() + 1;
						int qryLastInsertedNt = currentSegment.getQueryStart() - 1;
						// some query nucleotides were inserted.
						if(qryFirstInsertedNt <= qryLastInsertedNt) {
							// the length of both segments is at least the flankingNTs
							if(lastSegment.getCurrentLength() >= this.flankingNTs && currentSegment.getCurrentLength() >= this.flankingNTs) {
								// inserted region is within configured min / max lengths
								int insertedRegionLength = (qryLastInsertedNt - qryFirstInsertedNt) + 1;
								if( (minInsertionLengthNts == null || insertedRegionLength >= minInsertionLengthNts) && 
										(maxInsertionLengthNts == null || insertedRegionLength <= maxInsertionLengthNts) ) {
									String insertedQryNts = SegmentUtils.base1SubString(queryNts, qryFirstInsertedNt, qryLastInsertedNt);
									matchResults.add(new NucleotideInsertionMatchResult(
											refLastNtBeforeIns, refFirstNtAfterIns, 
											qryFirstInsertedNt, qryLastInsertedNt, 
											insertedQryNts));
								}
							}
						}
					}
				}
				lastSegment = currentSegment;
			}
		}
		return new VariationScanResult<NucleotideInsertionMatchResult>(this, sufficientCoverage, matchResults);
	}

	@Override
	public void validate() {
		super.validate();
		int ntStart = getVariation().getRefStart();
		int ntEnd = getVariation().getRefEnd();
		if(ntEnd <= ntStart) {
			throwScannerException("For nucleotide insertions start NT must be before end NT");
		}
	}
	
}
