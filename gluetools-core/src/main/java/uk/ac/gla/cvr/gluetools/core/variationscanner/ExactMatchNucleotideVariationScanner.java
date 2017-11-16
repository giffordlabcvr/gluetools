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

@PluginClass(elemName="exactMatchNucleotideVariationScanner",
description="Detects exact matches with a pattern when used with a nucleotide Variation")
public class ExactMatchNucleotideVariationScanner extends BaseNucleotideVariationScanner<ExactMatchNucleotideVariationScanner, VariationScanResult>{

	private static ExactMatchNucleotideVariationScanner defaultInstance = new ExactMatchNucleotideVariationScanner();
	
	@Override
	public VariationScanResult scanNucleotides(Variation variation, NtQueryAlignedSegment ntQaSeg) {
		List<PLocScanResult> pLocScanResults = new ArrayList<PLocScanResult>();

		for(int plocIdx = 0; plocIdx < variation.getPatternLocs().size(); plocIdx++) {
			PatternLocation pLoc = variation.getPatternLocs().get(plocIdx);
			PLocScanResult pLocScanResult;

			Integer refStart = pLoc.getRefStart();
			Integer refEnd = pLoc.getRefEnd();
			if(!( refStart >= ntQaSeg.getRefStart() && refEnd <= ntQaSeg.getRefEnd() )) {
				// query segment does not cover pattern loc
				pLocScanResult = new NucleotidePLocScanResult(plocIdx, Collections.emptyList(),
						Collections.emptyList()); // no match in this pattern loc
			} else {
				ReferenceSegment variationRegionSeg = new ReferenceSegment(refStart, refEnd);
				List<NtQueryAlignedSegment> intersection = ReferenceSegment.intersection(Arrays.asList(ntQaSeg), Arrays.asList(variationRegionSeg), 
						ReferenceSegment.cloneLeftSegMerger());
				if(intersection.isEmpty()) {
					// query segment does not cover pattern loc
					pLocScanResult = new NucleotidePLocScanResult(plocIdx, Collections.emptyList(),
							Collections.emptyList()); // no match in this pattern loc
				} else {
					NtQueryAlignedSegment intersectionSeg = intersection.get(0);
					CharSequence nucleotides = intersectionSeg.getNucleotides();
					Integer zeroIndexNtStart = intersectionSeg.getQueryStart();
					if(StringUtils.charSequencesEqual(pLoc.getPattern(), nucleotides)) {
						int ntStart = zeroIndexNtStart;
						int ntEnd = zeroIndexNtStart+nucleotides.length()-1;
						pLocScanResult = new NucleotidePLocScanResult(plocIdx, Arrays.asList(new ReferenceSegment(ntStart, ntEnd)),
								Arrays.asList(nucleotides.toString())); // single match in this pattern loc
					} else {
						pLocScanResult = new NucleotidePLocScanResult(plocIdx, Collections.emptyList(),
								Collections.emptyList()); // no match in this pattern loc
					}
				}
			}
			pLocScanResults.add(pLocScanResult);
		}
		return new VariationScanResult(variation, pLocScanResults);
	}

	public static ExactMatchNucleotideVariationScanner getDefaultInstance() {
		return defaultInstance;
	}

}
