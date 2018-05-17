package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.GlueException;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationMetatag.VariationMetatag.VariationMetatagType;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public class ConjunctionScanner extends BaseVariationScanner<ConjunctionMatchResult> {

	
	
	private static final List<VariationMetatagType> allowedMetatagTypes = Arrays.asList(
			VariationMetatagType.CONJUNCT_NAME_1, 
			VariationMetatagType.CONJUNCT_NAME_2, 
			VariationMetatagType.CONJUNCT_NAME_3, 
			VariationMetatagType.CONJUNCT_NAME_4, 
			VariationMetatagType.CONJUNCT_NAME_5);
	private static final List<VariationMetatagType> requiredMetatagTypes = Arrays.asList(VariationMetatagType.CONJUNCT_NAME_1);

	private static int MAX_CONJUNCT_INDEX = 5;

	private List<BaseVariationScanner<?>> conjunctScanners = new ArrayList<BaseVariationScanner<?>>();
	private int numConjuncts;
	private Integer refStart = Integer.MAX_VALUE, refEnd = Integer.MIN_VALUE;
	
	public ConjunctionScanner() {
		super(allowedMetatagTypes, requiredMetatagTypes);
	}

	@Override
	public void init(CommandContext cmdContext) {
		super.init(cmdContext);
		String featureName = getVariation().getFeatureLoc().getFeature().getName();
		String referenceName = getVariation().getFeatureLoc().getReferenceSequence().getName();
		for(int i = 1; i <= MAX_CONJUNCT_INDEX; i++) {
			String conjunctName_i = getStringMetatagValue(VariationMetatagType.valueOf("CONJUNCT_NAME_"+i));
			if(conjunctName_i == null) {
				break;
			}
			numConjuncts = i;
			Variation conjunctVariation_i = GlueDataObject.lookup(cmdContext, Variation.class, Variation.pkMap(referenceName, featureName, conjunctName_i));
			BaseVariationScanner<?> conjunctScanner = conjunctVariation_i.getScanner(cmdContext);
			this.refStart = Math.min(this.refStart, conjunctScanner.getRefStart());
			this.refEnd = Math.max(this.refEnd, conjunctScanner.getRefEnd());
			conjunctScanners.add(conjunctScanner);
		}
		
	}
	
	@Override
	public void validate() {
		super.validate();
		int lastNonNullConjunct = 1;
		for(int i = 2; i <= MAX_CONJUNCT_INDEX; i++) {
			String conjunctName_i = getStringMetatagValue(VariationMetatagType.valueOf("CONJUNCT_NAME_"+i));
			if(conjunctName_i != null) {
				if(lastNonNullConjunct < i - 1) {
					throwScannerException("Conjunction variation should use consecutive conjunct indices");
				}
				lastNonNullConjunct = i;
			}
		}
		for(int i = 1; i <= numConjuncts; i++) {
			try {
				conjunctScanners.get(i-1).validate();
			} catch(GlueException ge) {
				throwScannerException(ge, "Validation of conjunct "+i+" failed: "+ge.getLocalizedMessage());
			}
		}
		
	}

	public int getNumConjucts() {
		return numConjuncts;
	}

	
	
	@Override
	public List<ReferenceSegment> getSegmentsToCover() {
		List<ReferenceSegment> segmentsToCover = new ArrayList<ReferenceSegment>(conjunctScanners.get(0).getSegmentsToCover());
		for(int i = 2; i <= numConjuncts; i++) {
			List<ReferenceSegment> conjuctSegmentsToCover = conjunctScanners.get(i-1).getSegmentsToCover();
			
			List<ReferenceSegment> intersection = ReferenceSegment.intersection(conjuctSegmentsToCover, segmentsToCover, ReferenceSegment.cloneLeftSegMerger());
			conjuctSegmentsToCover = ReferenceSegment.subtract(conjuctSegmentsToCover, intersection);
			segmentsToCover.addAll(conjuctSegmentsToCover);
			ReferenceSegment.sortByRefStart(segmentsToCover);
			
		}
		segmentsToCover = ReferenceSegment.mergeAbutting(segmentsToCover, ReferenceSegment.mergeAbuttingFunctionReferenceSegment(), 
				ReferenceSegment.abutsPredicateReferenceSegment());
		return segmentsToCover;
	}

	@Override
	protected VariationScanResult<ConjunctionMatchResult> scanInternal(
			List<NtQueryAlignedSegment> queryToRefNtSegs,
			String queryNts, String qualityString) {
		boolean sufficientCoverage = computeSufficientCoverage(queryToRefNtSegs);
		if(!sufficientCoverage) {
			return new VariationScanResult<ConjunctionMatchResult>(getVariation(), refStart, refEnd, sufficientCoverage, Collections.emptyList());
		}
		ConjunctionMatchResult conjunctionMatchResult = new ConjunctionMatchResult();
		boolean isPresent = true;
		for(int i = 1; i <= numConjuncts; i++) {
			BaseVariationScanner<?> conjunctScanner = conjunctScanners.get(i-1);
			Class<? extends VariationScannerMatchResult> conjunctMatchResultClass = conjunctScanner.getVariation().getVariationType().getMatchResultClass();
			isPresent &= updateConjunctionMatchResult(conjunctMatchResultClass, conjunctionMatchResult, conjunctScanner, i, queryToRefNtSegs, queryNts, qualityString);
		}
		if(isPresent) {
			return new VariationScanResult<ConjunctionMatchResult>(getVariation(), refStart, refEnd, sufficientCoverage, Arrays.asList(conjunctionMatchResult));
		} else {
			return new VariationScanResult<ConjunctionMatchResult>(getVariation(), refStart, refEnd, sufficientCoverage, Collections.emptyList());
		}
	}
	
	private <D extends VariationScannerMatchResult> boolean updateConjunctionMatchResult(Class<D> conjunctMatchResultClass, 
			ConjunctionMatchResult conjunctionMatchResult, BaseVariationScanner<?> conjunctScanner, int conjunctIndex,
			List<NtQueryAlignedSegment> queryToRefNtSegs, String queryNts, String qualityString) {
		@SuppressWarnings("unchecked")
		BaseVariationScanner<D> castConjunctScanner = (BaseVariationScanner<D>) conjunctScanner;
		VariationScanResult<D> conjunctScanResult = castConjunctScanner.scan(queryToRefNtSegs, queryNts, qualityString);
		conjunctionMatchResult.setConjunctResults(conjunctIndex, conjunctScanResult);
		Integer currentWorstQScore = conjunctionMatchResult.getWorstContributingQScore();
		Integer conjunctLeastBadQScore = conjunctScanResult.getLeastBadMatchQScore();
		if(currentWorstQScore == null) {
			conjunctionMatchResult.setWorstContributingQScore(conjunctLeastBadQScore);
		} else {
			if(conjunctLeastBadQScore != null) {
				conjunctionMatchResult.setWorstContributingQScore(Math.min(currentWorstQScore, conjunctLeastBadQScore));
			}
		}
		
		return conjunctScanResult.isPresent();
	}

	@Override
	public Integer getRefStart() {
		return refStart;
	}

	@Override
	public Integer getRefEnd() {
		return refEnd;
	}
	
}
