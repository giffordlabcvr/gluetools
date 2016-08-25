package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import uk.ac.gla.cvr.gluetools.core.datamodel.patternlocation.PatternLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.VariationException.Code;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public class RegexNucleotideVariationScanner extends BaseNucleotideVariationScanner<RegexNucleotideVariationScanner, VariationScanResult>{

	private static RegexNucleotideVariationScanner defaultInstance = new RegexNucleotideVariationScanner();
	
	@Override
	public void validateVariation(Variation variation) {
		super.validateVariation(variation);
		variation.getPatternLocs().forEach(pLoc -> {parseRegex(pLoc);});
	}

	@Override
	public VariationScanResult scanNucleotides(Variation variation, NtQueryAlignedSegment ntQaSeg) {
		List<ReferenceSegment> queryLocs = new ArrayList<ReferenceSegment>();
		
		boolean result = true;

		for(PatternLocation pLoc : variation.getPatternLocs()) {
			Integer refStart = pLoc.getRefStart();
			Integer refEnd = pLoc.getRefEnd();
			if(!( refStart >= ntQaSeg.getRefStart() && refEnd <= ntQaSeg.getRefEnd() )) {
				return null;
			}
			ReferenceSegment variationRegionSeg = new ReferenceSegment(refStart, refEnd);
			List<NtQueryAlignedSegment> intersection = ReferenceSegment.intersection(Arrays.asList(ntQaSeg), Arrays.asList(variationRegionSeg), 
					ReferenceSegment.cloneLeftSegMerger());
			if(intersection.isEmpty()) {
				return null;
			}

			Pattern regexPattern = (Pattern) pLoc.getScannerData("NT_REGEX_PATTERN");
			if(regexPattern == null) {
				regexPattern = parseRegex(pLoc);
				pLoc.setScannerData("NT_REGEX_PATTERN", regexPattern);
			}
			
			NtQueryAlignedSegment intersectionSeg = intersection.get(0);
			CharSequence nucleotides = intersectionSeg.getNucleotides();
			Integer zeroIndexNtStart = intersectionSeg.getQueryStart();

			Matcher matcher = regexPattern.matcher(nucleotides);
			if(result && matcher.find()) {
				int ntStart = zeroIndexNtStart + matcher.start();
				int ntEnd = zeroIndexNtStart + matcher.end() - 1;
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
	
	public static RegexNucleotideVariationScanner getDefaultInstance() {
		return defaultInstance;
	}
	
}
