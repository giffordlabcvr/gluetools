package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import uk.ac.gla.cvr.gluetools.core.datamodel.patternlocation.PatternLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

@PluginClass(elemName="regexAminoAcidVariationScanner")
public class RegexAminoAcidVariationScanner extends BaseAminoAcidVariationScanner<RegexAminoAcidVariationScanner, VariationScanResult>{

	private static RegexAminoAcidVariationScanner defaultInstance = new RegexAminoAcidVariationScanner();
	
	@Override
	public void validateVariation(Variation variation) {
		super.validateVariation(variation);
		variation.getPatternLocs().forEach(pLoc -> {parseRegex(pLoc);});
	}

	
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
			
			Pattern regexPattern = (Pattern) pLoc.getScannerData("AA_REGEX_PATTERN");
			if(regexPattern == null) {
				regexPattern = parseRegex(pLoc);
				pLoc.setScannerData("AA_REGEX_PATTERN", regexPattern);
			}
			Matcher matcher = regexPattern.matcher(aminoAcids);
			if(result && matcher.find()) {
				int ntStart = scanQueryNtStart + ( matcher.start() * 3 );
				int ntEnd = scanQueryNtStart + ( matcher.end() * 3 ) - 1;
				queryLocs.add(new ReferenceSegment(ntStart, ntEnd));
			} else {
				result = false;
				queryLocs.clear();
			}
		}
		return new VariationScanResult(variation, result, queryLocs);
	}

	
	private Pattern parseRegex(PatternLocation pLoc) {
		try {
			return Pattern.compile(pLoc.getPattern());
		} catch(PatternSyntaxException pse) {
			Variation variation = pLoc.getVariation();
			throw new VariationException(pse, Code.VARIATION_SCANNER_EXCEPTION, 
					variation.getFeatureLoc().getReferenceSequence().getName(), 
					variation.getFeatureLoc().getFeature().getName(), variation.getName(), 
					"Syntax error in variation regex: "+pse.getMessage());
		}
	}
	
	public static RegexAminoAcidVariationScanner getDefaultInstance() {
		return defaultInstance;
	}
	
}
