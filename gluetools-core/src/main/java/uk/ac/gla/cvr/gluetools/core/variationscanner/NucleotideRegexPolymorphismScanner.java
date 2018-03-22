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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationMetatag.VariationMetatag.VariationMetatagType;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public class NucleotideRegexPolymorphismScanner extends BaseNucleotideVariationScanner<NucleotideRegexPolymorphismMatchResult> {

	private static final List<VariationMetatagType> allowedMetatagTypes = 
			Arrays.asList(VariationMetatagType.REGEX_NT_PATTERN,
							VariationMetatagType.MIN_COVERAGE_NTS);
	private static final List<VariationMetatagType> requiredMetatagTypes = Arrays.asList(VariationMetatagType.REGEX_NT_PATTERN);

	private String regexNtPattern;
	
	public NucleotideRegexPolymorphismScanner() {
		super(allowedMetatagTypes, requiredMetatagTypes);
	}
	
	@Override
	protected void init(CommandContext cmdContext) {
		super.init(cmdContext);		
		this.regexNtPattern = getStringMetatagValue(VariationMetatagType.REGEX_NT_PATTERN);
	}

	@Override
	protected VariationScanResult<NucleotideRegexPolymorphismMatchResult> scanInternal(
			CommandContext cmdContext,
			List<NtQueryAlignedSegment> queryToRefNtSegs) {
		List<NucleotideRegexPolymorphismMatchResult> matchResults = new ArrayList<NucleotideRegexPolymorphismMatchResult>();
		boolean sufficientCoverage = computeSufficientCoverage(queryToRefNtSegs);
		if(sufficientCoverage) {
			Variation variation = getVariation();

			List<NtQueryAlignedSegment> queryToRefNtSegsVariationRegion = 
					ReferenceSegment.intersection(queryToRefNtSegs, Arrays.asList(new ReferenceSegment(variation.getRefStart(), variation.getRefEnd())), 
							ReferenceSegment.cloneLeftSegMerger());

			Pattern pattern = parseRegex(regexNtPattern);
			for(NtQueryAlignedSegment ntQaSeg: queryToRefNtSegsVariationRegion) {
				Matcher matcher = pattern.matcher(ntQaSeg.getNucleotides());
				while(matcher.find()) {
					int queryNtStart = ntQaSeg.getQueryStart() + matcher.start();
					int queryNtEnd = ntQaSeg.getQueryStart() + matcher.end()-1;
					int refNtStart = queryNtStart + ntQaSeg.getQueryToReferenceOffset();
					int refNtEnd = queryNtEnd + ntQaSeg.getQueryToReferenceOffset();
					String queryNts = matcher.group();
					NucleotideRegexPolymorphismMatchResult npmr = 
							new NucleotideRegexPolymorphismMatchResult(refNtStart, refNtEnd, queryNtStart, queryNtEnd, queryNts);
					matchResults.add(npmr);
				}
			}
		}
		return new VariationScanResult<NucleotideRegexPolymorphismMatchResult>(getVariation(), sufficientCoverage, matchResults);

	}
	
}
