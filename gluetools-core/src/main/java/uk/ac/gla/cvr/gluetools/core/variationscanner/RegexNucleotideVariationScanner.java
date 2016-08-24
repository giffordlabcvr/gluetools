package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException.Code;

public class RegexNucleotideVariationScanner extends BaseNucleotideVariationScanner<RegexNucleotideVariationScanner, VariationScanResult>{

	private static RegexNucleotideVariationScanner defaultInstance = new RegexNucleotideVariationScanner();
	
	@Override
	public void validateVariation(Variation variation) {
		super.validateVariation(variation);
		parseRegex(variation);
	}

	@Override
	public VariationScanResult scanNucleotides(Variation variation, CharSequence nucleotides, int zeroIndexNtStart) {
		boolean result;
		Integer ntStart = null;
		Integer ntEnd = null;
		Pattern regexPattern = (Pattern) variation.getScannerData("NT_REGEX_PATTERN");
		if(regexPattern == null) {
			regexPattern = parseRegex(variation);
			variation.setScannerData("NT_REGEX_PATTERN", regexPattern);
		}
		Matcher matcher = regexPattern.matcher(nucleotides);
		result = matcher.find();
		if(result) {
			ntStart = zeroIndexNtStart + matcher.start();
			ntEnd = zeroIndexNtStart + matcher.end() - 1;
		}
		return new VariationScanResult(variation, result, ntStart, ntEnd);
	}

	private Pattern parseRegex(Variation variation) {
		try {
			return Pattern.compile(variation.getPattern());
		} catch(PatternSyntaxException pse) {
			throw new VariationException(pse, Code.VARIATION_SCANNER_EXCEPTION, 
					variation.getFeatureLoc().getReferenceSequence().getName(), 
					variation.getFeatureLoc().getFeature().getName(), variation.getName(), 
					"Syntax error in variation regex: "+pse.getMessage());
		}
	}
	
	public static RegexNucleotideVariationScanner getDefaultInstance() {
		return defaultInstance;
	}
	
}
