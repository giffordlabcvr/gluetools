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

public class AminoAcidInsertionScanner extends BaseAminoAcidVariationScanner<AminoAcidInsertionMatchResult> {

	private static final List<VariationMetatagType> allowedMetatagTypes = Arrays.asList(VariationMetatagType.FLANKING_AAS, VariationMetatagType.FLANKING_NTS);
	private static final List<VariationMetatagType> requiredMetatagTypes = Arrays.asList();

	public AminoAcidInsertionScanner() {
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
	public VariationScanResult<AminoAcidInsertionMatchResult> scan(CommandContext cmdContext, List<NtQueryAlignedSegment> queryToRefNtSegs) {
		List<AminoAcidInsertionMatchResult> matchResults = new ArrayList<AminoAcidInsertionMatchResult>();
		boolean sufficientCoverage = computeSufficientCoverage(queryToRefNtSegs);
		
		return new VariationScanResult<AminoAcidInsertionMatchResult>(getVariation(), sufficientCoverage, matchResults);
	}

	
}
