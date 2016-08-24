package uk.ac.gla.cvr.gluetools.core.variationscanner;

import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.utils.StringUtils;

public class ExactMatchNucleotideVariationScanner extends BaseNucleotideVariationScanner<ExactMatchNucleotideVariationScanner, VariationScanResult>{

	private static ExactMatchNucleotideVariationScanner defaultInstance = new ExactMatchNucleotideVariationScanner();
	
	@Override
	public VariationScanResult scanNucleotides(Variation variation, CharSequence nucleotides, int zeroIndexNtStart) {
		boolean result;
		Integer ntStart = null;
		Integer ntEnd = null;
		result = StringUtils.charSequencesEqual(variation.getPattern(), nucleotides);
		if(result) {
			ntStart = zeroIndexNtStart;
			ntEnd = zeroIndexNtStart+nucleotides.length()-1;
		}
		return new VariationScanResult(variation, result, ntStart, ntEnd);
	}

	public static ExactMatchNucleotideVariationScanner getDefaultInstance() {
		return defaultInstance;
	}

}
