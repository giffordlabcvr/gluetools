package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.datamodel.patternlocation.PatternLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.utils.StringUtils;

public class ExactMatchNucleotideVariationScanner extends BaseNucleotideVariationScanner<ExactMatchNucleotideVariationScanner, VariationScanResult>{

	private static ExactMatchNucleotideVariationScanner defaultInstance = new ExactMatchNucleotideVariationScanner();
	
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
			NtQueryAlignedSegment intersectionSeg = intersection.get(0);
			CharSequence nucleotides = intersectionSeg.getNucleotides();
			Integer zeroIndexNtStart = intersectionSeg.getQueryStart();
			if(result && StringUtils.charSequencesEqual(pLoc.getPattern(), nucleotides)) {
				int ntStart = zeroIndexNtStart;
				int ntEnd = zeroIndexNtStart+nucleotides.length()-1;
				queryLocs.add(new ReferenceSegment(ntStart, ntEnd));
			} else {
				result = false;
				queryLocs.clear();
			}
		}
		return new VariationScanResult(variation, result, queryLocs);
	}

	public static ExactMatchNucleotideVariationScanner getDefaultInstance() {
		return defaultInstance;
	}

}
