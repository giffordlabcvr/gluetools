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
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationMetatag.VariationMetatag.VariationMetatagType;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;


public class AminoAcidDeletionScanner extends BaseAminoAcidVariationScanner<AminoAcidDeletionMatchResult> {

	private static final List<VariationMetatagType> allowedMetatagTypes = Arrays.asList(VariationMetatagType.FLANKING_AAS, VariationMetatagType.FLANKING_NTS);
	private static final List<VariationMetatagType> requiredMetatagTypes = Arrays.asList();

	private int flankingNTs;
	private String referenceNucleotides;
	
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
		this.referenceNucleotides = getVariation().getFeatureLoc().getReferenceSequence()
				.getSequence().getSequenceObject().getNucleotides(cmdContext);
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
	}
	
	protected boolean computeSufficientCoverage(List<NtQueryAlignedSegment> queryToRefNtSegs) {
		Integer flankingStart = computeFlankingStart();
		Integer flankingEnd = computeFlankingEnd();
		return ReferenceSegment.covers(queryToRefNtSegs, Arrays.asList(new ReferenceSegment(flankingStart, flankingEnd)));
	}

	private Integer computeFlankingStart() {
		return Math.max(getVariation().getRefStart()-this.flankingNTs, 1);
	}
	private Integer computeFlankingEnd() {
		return Math.min(getVariation().getRefEnd(), this.referenceNucleotides.length());
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
					int queryLastNtBeforeDel = lastSegment.getQueryEnd();
					int queryFirstNtAfterDel = currentSegment.getQueryStart();
					// query segments are abutting
					if(queryLastNtBeforeDel == (queryFirstNtAfterDel-1)) {
						
					}
				}
				lastSegment = currentSegment;
			}
		}
		return new VariationScanResult<AminoAcidDeletionMatchResult>(getVariation(), sufficientCoverage, matchResults);
	}


}
