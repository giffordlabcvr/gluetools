package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

@PluginClass(elemName="regexNucleotideVariationScanner")
public class RegexNucleotideVariationScanner extends BaseNucleotideVariationScanner<RegexNucleotideVariationScanner, VariationScanResult>{

	private static RegexNucleotideVariationScanner defaultInstance = new RegexNucleotideVariationScanner();
	
	@Override
	public void validateVariation(Variation variation) {
		super.validateVariation(variation);
		variation.getPatternLocs().forEach(pLoc -> {parseRegex(pLoc);});
	}

	@Override
	public VariationScanResult scanNucleotides(Variation variation, NtQueryAlignedSegment ntQaSeg) {
		List<PLocScanResult> pLocScanResults = new ArrayList<PLocScanResult>();

		for(int plocIdx = 0; plocIdx < variation.getPatternLocs().size(); plocIdx++) {
			PatternLocation pLoc = variation.getPatternLocs().get(plocIdx);
			PLocScanResult pLocScanResult;

			Integer refStart = pLoc.getRefStart();
			Integer refEnd = pLoc.getRefEnd();
			if(!( refStart >= ntQaSeg.getRefStart() && refEnd <= ntQaSeg.getRefEnd() )) {
				pLocScanResult = new NucleotidePLocScanResult(plocIdx, Collections.emptyList(),
						Collections.emptyList()); // no match in this pattern loc
			} else {
				ReferenceSegment variationRegionSeg = new ReferenceSegment(refStart, refEnd);
				List<NtQueryAlignedSegment> intersection = ReferenceSegment.intersection(Arrays.asList(ntQaSeg), Arrays.asList(variationRegionSeg), 
						ReferenceSegment.cloneLeftSegMerger());
				if(intersection.isEmpty()) {
					pLocScanResult = new NucleotidePLocScanResult(plocIdx, Collections.emptyList(),
							Collections.emptyList()); // no match in this pattern loc
				} else {
					// set up caching of Pattern in PatternLoc.
					Pattern regexPattern = (Pattern) pLoc.getScannerData("NT_REGEX_PATTERN");
					if(regexPattern == null) {
						regexPattern = parseRegex(pLoc);
						pLoc.setScannerData("NT_REGEX_PATTERN", regexPattern);
					}
					
					NtQueryAlignedSegment intersectionSeg = intersection.get(0);
					CharSequence nucleotides = intersectionSeg.getNucleotides();
					Integer zeroIndexNtStart = intersectionSeg.getQueryStart();
		
					List<ReferenceSegment> queryLocs = new ArrayList<ReferenceSegment>();
					List<String> ntMatchValues = new ArrayList<String>();
					
					Matcher matcher = regexPattern.matcher(nucleotides);
					while(matcher.find()) {
						int ntStart = zeroIndexNtStart + matcher.start();
						int ntEnd = zeroIndexNtStart + matcher.end() - 1;
						queryLocs.add(new ReferenceSegment(ntStart, ntEnd));
						ntMatchValues.add(matcher.group());
					} 
					pLocScanResult = new AminoAcidPLocScanResult(plocIdx, queryLocs, ntMatchValues);
				}
			}
			pLocScanResults.add(pLocScanResult);
		}
		return new VariationScanResult(variation, pLocScanResults);
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
