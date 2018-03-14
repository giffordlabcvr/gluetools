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
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public class NucleotideDeletionScanner extends BaseNucleotideVariationScanner<NucleotideDeletionMatchResult> {

	private static final List<VariationMetatagType> allowedMetatagTypes = Arrays.asList(
			VariationMetatagType.FLANKING_NTS, 
			VariationMetatagType.MIN_DELETION_LENGTH_NTS,
			VariationMetatagType.MAX_DELETION_LENGTH_NTS,
			VariationMetatagType.MIN_COVERAGE_NTS);
	private static final List<VariationMetatagType> requiredMetatagTypes = Arrays.asList();

	private int flankingNTs;
	private String referenceNucleotides;
	private Integer minDeletionLengthNts;
	private Integer maxDeletionLengthNts;
	
	public NucleotideDeletionScanner() {
		super(allowedMetatagTypes, requiredMetatagTypes);
	}

	@Override
	public void init(CommandContext cmdContext) {
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
			this.maxDeletionLengthNts = null;
		}
	}
	
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
		return Math.min(getVariation().getRefEnd(), this.referenceNucleotides.length());
	}

	
	@Override
	public VariationScanResult<NucleotideDeletionMatchResult> scan(CommandContext cmdContext, List<NtQueryAlignedSegment> queryToRefNtSegs) {
		List<NucleotideDeletionMatchResult> matchResults = new ArrayList<NucleotideDeletionMatchResult>();
		boolean sufficientCoverage = computeSufficientCoverage(queryToRefNtSegs);
		
		return new VariationScanResult<NucleotideDeletionMatchResult>(getVariation(), sufficientCoverage, matchResults);
	}

}
