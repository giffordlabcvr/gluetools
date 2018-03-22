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
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationMetatag.VariationMetatag.VariationMetatagType;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public class NucleotideSimplePolymorphismScanner extends BaseNucleotideVariationScanner<NucleotideSimplePolymorphismMatchResult> {

	private static final List<VariationMetatagType> allowedMetatagTypes = 
			Arrays.asList(VariationMetatagType.SIMPLE_NT_PATTERN, VariationMetatagType.MIN_COVERAGE_NTS);
	private static final List<VariationMetatagType> requiredMetatagTypes = Arrays.asList(VariationMetatagType.SIMPLE_NT_PATTERN);

	private String simpleNtPattern;
	
	public NucleotideSimplePolymorphismScanner() {
		super(allowedMetatagTypes, requiredMetatagTypes);
	}
	
	@Override
	protected void init(CommandContext cmdContext) {
		super.init(cmdContext);		
		this.simpleNtPattern = getStringMetatagValue(VariationMetatagType.SIMPLE_NT_PATTERN);
	}


	@Override
	public VariationScanResult<NucleotideSimplePolymorphismMatchResult> scan(
			CommandContext cmdContext,
			List<NtQueryAlignedSegment> queryToRefNtSegs) {
		List<NucleotideSimplePolymorphismMatchResult> matchResults = new ArrayList<NucleotideSimplePolymorphismMatchResult>();
		boolean sufficientCoverage = computeSufficientCoverage(queryToRefNtSegs);
		if(sufficientCoverage) {
			Variation variation = getVariation();

			List<NtQueryAlignedSegment> queryToRefNtSegsVariationRegion = 
					ReferenceSegment.intersection(queryToRefNtSegs, Arrays.asList(new ReferenceSegment(variation.getRefStart(), variation.getRefEnd())), 
							ReferenceSegment.cloneLeftSegMerger());


			for(NtQueryAlignedSegment ntQaSeg: queryToRefNtSegsVariationRegion) {
				String segNts = ntQaSeg.getNucleotides().toString();
				int nextIndex = -1;
				do {
					nextIndex = segNts.indexOf(simpleNtPattern, nextIndex+1);
					if(nextIndex >= 0) {
						int queryNtStart = ntQaSeg.getQueryStart() + nextIndex;
						int queryNtEnd = queryNtStart + (simpleNtPattern.length()-1);
						int refNtStart = queryNtStart + ntQaSeg.getQueryToReferenceOffset();
						int refNtEnd = queryNtEnd + ntQaSeg.getQueryToReferenceOffset();
						String queryNts = segNts.substring(nextIndex, nextIndex+simpleNtPattern.length());
						NucleotideSimplePolymorphismMatchResult nspmr = 
								new NucleotideSimplePolymorphismMatchResult(refNtStart, refNtEnd, queryNtStart, queryNtEnd, queryNts);
						matchResults.add(nspmr);
					}
				} while(nextIndex != -1);
			}
		}
		return new VariationScanResult<NucleotideSimplePolymorphismMatchResult>(getVariation(), sufficientCoverage, matchResults);

	}
	
}
