package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.datamodel.patternlocation.PatternLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.utils.StringUtils;

@PluginClass(elemName="exactMatchAminoAcidVariationScanner")
public class ExactMatchAminoAcidVariationScanner extends BaseAminoAcidVariationScanner<ExactMatchAminoAcidVariationScanner, VariationScanResult>{

	private static ExactMatchAminoAcidVariationScanner defaultInstance = new ExactMatchAminoAcidVariationScanner();
	
	@Override
	public VariationScanResult scanAminoAcids(Variation variation, NtQueryAlignedSegment ntQaSegCdnAligned, String fullAminoAcidTranslation) {

		List<PLocScanResult> pLocScanResults = new ArrayList<PLocScanResult>();
		
		for(PatternLocation pLoc : variation.getPatternLocs()) {
			PLocScanResult pLocScanResult;
			Integer refStart = pLoc.getRefStart();
			Integer refEnd = pLoc.getRefEnd();
			int varLengthNt = refEnd - refStart + 1;
			Integer aaTranslationRefNtStart = ntQaSegCdnAligned.getRefStart();
			Integer aaTranslationRefNtEnd = ntQaSegCdnAligned.getRefEnd();
			if(!( refStart >= aaTranslationRefNtStart && refEnd <= aaTranslationRefNtEnd )) {
				// pattern location is outside query translation.
				pLocScanResult = new AminoAcidPLocScanResult(Collections.emptyList(),
						Collections.emptyList()); // no match in this pattern loc
			} else {
				int segToVariationStartOffset = refStart - aaTranslationRefNtStart;
				int startAA = segToVariationStartOffset / 3;
				int endAA = startAA + ( (varLengthNt / 3) - 1);
				CharSequence aminoAcids = fullAminoAcidTranslation.subSequence(startAA, endAA+1);
				int scanQueryNtStart = ntQaSegCdnAligned.getQueryStart() + segToVariationStartOffset;

				if(StringUtils.charSequencesEqual(pLoc.getPattern(), aminoAcids)) {
					int ntStart = scanQueryNtStart;
					int ntEnd = scanQueryNtStart+(aminoAcids.length() * 3)-1;
					pLocScanResult = new AminoAcidPLocScanResult(Arrays.asList(new ReferenceSegment(ntStart, ntEnd)),
							Arrays.asList(aminoAcids.toString())); // single match in this pattern loc
				} else {
					pLocScanResult = new AminoAcidPLocScanResult(Collections.emptyList(),
							Collections.emptyList()); // no match in this pattern loc
				}
			}
			pLocScanResults.add(pLocScanResult);
		}
		return new VariationScanResult(variation, pLocScanResults);
	}

	public static ExactMatchAminoAcidVariationScanner getDefaultInstance() {
		return defaultInstance;
	}
	
}
