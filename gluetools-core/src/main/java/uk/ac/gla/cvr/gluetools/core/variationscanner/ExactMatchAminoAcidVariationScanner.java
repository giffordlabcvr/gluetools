package uk.ac.gla.cvr.gluetools.core.variationscanner;

import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.utils.StringUtils;

public class ExactMatchAminoAcidVariationScanner extends BaseAminoAcidVariationScanner<ExactMatchAminoAcidVariationScanner, VariationScanResult>{

	private static ExactMatchAminoAcidVariationScanner defaultInstance = new ExactMatchAminoAcidVariationScanner();
	
	@Override
	public VariationScanResult scanAminoAcids(Variation variation, CharSequence aminoAcids, int zeroIndexNtStart) {
		boolean result;
		Integer ntStart = null;
		Integer ntEnd = null;
		result = StringUtils.charSequencesEqual(variation.getPattern(), aminoAcids);
		if(result) {
			ntStart = zeroIndexNtStart;
			ntEnd = zeroIndexNtStart+(aminoAcids.length() * 3)-1;
		}
		return new VariationScanResult(variation, result, ntStart, ntEnd);
	}

	public static ExactMatchAminoAcidVariationScanner getDefaultInstance() {
		return defaultInstance;
	}
	
}
