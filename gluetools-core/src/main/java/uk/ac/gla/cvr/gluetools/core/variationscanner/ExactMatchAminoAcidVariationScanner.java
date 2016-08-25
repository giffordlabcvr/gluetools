package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.datamodel.patternlocation.PatternLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.utils.StringUtils;

public class ExactMatchAminoAcidVariationScanner extends BaseAminoAcidVariationScanner<ExactMatchAminoAcidVariationScanner, VariationScanResult>{

	private static ExactMatchAminoAcidVariationScanner defaultInstance = new ExactMatchAminoAcidVariationScanner();
	
	@Override
	public VariationScanResult scanAminoAcids(Variation variation, NtQueryAlignedSegment ntQaSegCdnAligned, String fullAminoAcidTranslation) {

		boolean result = true;
		
		List<ReferenceSegment> queryLocs = new ArrayList<ReferenceSegment>();
		
		for(PatternLocation pLoc : variation.getPatternLocs()) {
			Integer refStart = pLoc.getRefStart();
			Integer refEnd = pLoc.getRefEnd();
			int varLengthNt = refEnd - refStart + 1;
			Integer aaTranslationRefNtStart = ntQaSegCdnAligned.getRefStart();
			Integer aaTranslationRefNtEnd = ntQaSegCdnAligned.getRefEnd();
			if(!( refStart >= aaTranslationRefNtStart && refEnd <= aaTranslationRefNtEnd )) {
				return null;
			}
			int segToVariationStartOffset = refStart - aaTranslationRefNtStart;
			int startAA = segToVariationStartOffset / 3;
			int endAA = startAA + ( (varLengthNt / 3) - 1);
			CharSequence aminoAcids = fullAminoAcidTranslation.subSequence(startAA, endAA+1);
			int scanQueryNtStart = ntQaSegCdnAligned.getQueryStart() + segToVariationStartOffset;

			if(result && StringUtils.charSequencesEqual(pLoc.getPattern(), aminoAcids)) {
				int ntStart = scanQueryNtStart;
				int ntEnd = scanQueryNtStart+(aminoAcids.length() * 3)-1;
				queryLocs.add(new ReferenceSegment(ntStart, ntEnd));
			} else {
				result = false;
				queryLocs.clear();
			}
		}
		return new VariationScanResult(variation, result, queryLocs);
	}

	public static ExactMatchAminoAcidVariationScanner getDefaultInstance() {
		return defaultInstance;
	}
	
}
