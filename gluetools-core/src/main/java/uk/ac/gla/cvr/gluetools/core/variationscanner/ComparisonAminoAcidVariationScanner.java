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
import java.util.Collections;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.patternlocation.PatternLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

@PluginClass(elemName="comparisonAminoAcidVariationScanner",
		description="Reports differences from a pattern when used with an amino acid Variation")
public class ComparisonAminoAcidVariationScanner extends BaseAminoAcidVariationScanner<ComparisonAminoAcidVariationScanner, VariationScanResult>{

	
	
	@Override
	public VariationScanResult scanAminoAcids(CommandContext cmdContext, Variation variation,
			NtQueryAlignedSegment ntQaSegCdnAligned,
			String fullAminoAcidTranslation) {
	
		
		List<PLocScanResult> pLocScanResults = new ArrayList<PLocScanResult>();
		
		for(int plocIdx = 0; plocIdx < variation.getPatternLocs().size(); plocIdx++) {
			PatternLocation pLoc = variation.getPatternLocs().get(plocIdx);
			PLocScanResult pLocScanResult;
			Integer refStart = pLoc.getRefStart();
			Integer refEnd = pLoc.getRefEnd();
			
			int varLengthNt = refEnd - refStart + 1;
			Integer aaTranslationRefNtStart = ntQaSegCdnAligned.getRefStart();
			Integer aaTranslationRefNtEnd = ntQaSegCdnAligned.getRefEnd();
			
			if(!( refStart >= aaTranslationRefNtStart && refEnd <= aaTranslationRefNtEnd )) {
				// pattern location is outside query translation.
				pLocScanResult = new AminoAcidPLocScanResult(plocIdx, Collections.emptyList(),
						Collections.emptyList(), Collections.emptyList(), Collections.emptyList()); // no match in this pattern loc
			} else {
				int segToVariationStartOffset = refStart - aaTranslationRefNtStart;
				int startAA = segToVariationStartOffset / 3;
				int endAA = startAA + ( (varLengthNt / 3) - 1);
				CharSequence queryAminoAcids = fullAminoAcidTranslation.subSequence(startAA, endAA+1);

				int scanQueryNtStart = ntQaSegCdnAligned.getQueryStart() + segToVariationStartOffset;

				
				int queryNtStart = scanQueryNtStart;
				int queryNtEnd = scanQueryNtStart+(queryAminoAcids.length() * 3)-1;
				int refNtStart = queryNtStart + ntQaSegCdnAligned.getQueryToReferenceOffset();
				int refNtEnd = queryNtEnd + ntQaSegCdnAligned.getQueryToReferenceOffset();
				TIntObjectMap<LabeledCodon> refNtToLabeledCodon = variation.getFeatureLoc().getRefNtToLabeledCodon(cmdContext);
				String lcStart = refNtToLabeledCodon.get(refNtStart).getCodonLabel();
				String lcEnd = refNtToLabeledCodon.get(refNtEnd-2).getCodonLabel();
				
				pLocScanResult = new AminoAcidPLocScanResult(plocIdx, Arrays.asList(new ReferenceSegment(queryNtStart, queryNtEnd)), 
						Arrays.asList(queryAminoAcids.toString()), 
						Arrays.asList(lcStart),
						Arrays.asList(lcEnd)); // single match
			}
			pLocScanResults.add(pLocScanResult);
		}
		return new VariationScanResult(variation, pLocScanResults);
	}

}
