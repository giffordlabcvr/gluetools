package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public class FeatureReferenceSegment extends ReferenceSegment {

	private String featureName;

	public FeatureReferenceSegment(String featureName, int refStart, int refEnd) {
		super(refStart, refEnd);
		this.featureName = featureName;
	}

	public String getFeatureName() {
		return featureName;
	}
	
	public FeatureReferenceSegment clone() {
		return new FeatureReferenceSegment(featureName, getRefStart(), getRefEnd());
	}
	
	public static BiFunction<FeatureReferenceSegment, FeatureReferenceSegment, FeatureReferenceSegment> mergeAbuttingFunctionFeatureReferenceSegment() {
		return (seg1, seg2) -> {
			return new FeatureReferenceSegment(seg1.featureName, seg1.getRefStart(), seg2.getRefEnd());
		};
	}
	
	public static BiPredicate<FeatureReferenceSegment, FeatureReferenceSegment> abutsPredicateFeatureReferenceSegment() {
		return (seg1, seg2) -> {
			return seg2.getRefStart() == seg1.getRefEnd()+1 && seg2.getFeatureName().equals(seg1.getFeatureName());
		};
	}

	
}
