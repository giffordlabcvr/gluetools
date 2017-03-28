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

@PluginClass(elemName="comparisonAminoAcidVariationScanner")
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
