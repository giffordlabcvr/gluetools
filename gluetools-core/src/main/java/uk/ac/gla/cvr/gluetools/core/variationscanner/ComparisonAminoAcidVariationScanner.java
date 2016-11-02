package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.datamodel.patternlocation.PatternLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.variationscanner.ComparisonAminoAcidVariationScanResult.PLocScanResult;

@PluginClass(elemName="comparisonAminoAcidVariationScanner")
public class ComparisonAminoAcidVariationScanner extends BaseAminoAcidVariationScanner<ComparisonAminoAcidVariationScanner, ComparisonAminoAcidVariationScanResult>{

	
	
	@Override
	public ComparisonAminoAcidVariationScanResult scanAminoAcids(Variation variation,
			NtQueryAlignedSegment ntQaSegCdnAligned,
			String fullAminoAcidTranslation) {
	
		
		List<ReferenceSegment> queryLocs = new ArrayList<ReferenceSegment>();
		ComparisonAminoAcidVariationScanResult result = new ComparisonAminoAcidVariationScanResult(variation, queryLocs);
		
		int pLocIndex = 0;
		for(PatternLocation pLoc : variation.getPatternLocs()) {
			Integer refStart = pLoc.getRefStart();
			Integer refEnd = pLoc.getRefEnd();
			
			int varLengthNt = refEnd - refStart + 1;
			Integer aaTranslationRefNtStart = ntQaSegCdnAligned.getRefStart();
			Integer aaTranslationRefNtEnd = ntQaSegCdnAligned.getRefEnd();
			
			if(!( refStart >= aaTranslationRefNtStart && refEnd <= aaTranslationRefNtEnd )) {
				return null; // pattern location is outside query translation.
			}
			int segToVariationStartOffset = refStart - aaTranslationRefNtStart;
			int startAA = segToVariationStartOffset / 3;
			int endAA = startAA + ( (varLengthNt / 3) - 1);
			CharSequence queryAminoAcids = fullAminoAcidTranslation.subSequence(startAA, endAA+1);

			int scanQueryNtStart = ntQaSegCdnAligned.getQueryStart() + segToVariationStartOffset;
			int ntStart = scanQueryNtStart;
			int ntEnd = scanQueryNtStart+(queryAminoAcids.length() * 3)-1;
			queryLocs.add(new ReferenceSegment(ntStart, ntEnd));
			
			result.getPLocScanResults()[pLocIndex] = new PLocScanResult(queryAminoAcids.toString());
			pLocIndex++;
		}
		return result;
	}

}
