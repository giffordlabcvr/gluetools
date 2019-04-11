package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import htsjdk.samtools.SAMRecord;
import uk.ac.gla.cvr.gluetools.core.GlueException;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.variation.Variation;
import uk.ac.gla.cvr.gluetools.core.datamodel.variationMetatag.VariationMetatag.VariationMetatagType;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public class ConjunctionScanner extends BaseVariationScanner<ConjunctionMatchResult> {

	
	
	private static final List<VariationMetatagType> allowedMetatagTypes = Arrays.asList(
			VariationMetatagType.CONJUNCT_NAME_1, 
			VariationMetatagType.CONJUNCT_NAME_2, 
			VariationMetatagType.CONJUNCT_NAME_3, 
			VariationMetatagType.CONJUNCT_NAME_4, 
			VariationMetatagType.CONJUNCT_NAME_5,
			VariationMetatagType.CONJUNCT_NAME_6, 
			VariationMetatagType.CONJUNCT_NAME_7, 
			VariationMetatagType.CONJUNCT_NAME_8, 
			VariationMetatagType.CONJUNCT_NAME_9, 
			VariationMetatagType.CONJUNCT_NAME_10);
	private static final List<VariationMetatagType> requiredMetatagTypes = Arrays.asList(VariationMetatagType.CONJUNCT_NAME_1);

	private static int MAX_CONJUNCT_INDEX = 10;

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
			CommandContext cmdContext, 
			List<QueryAlignedSegment> queryToRefSegs,
			String queryNts, String qualityString) {
		boolean sufficientCoverage = computeSufficientCoverage(queryToRefSegs);
		ConjunctionMatchResult conjunctionMatchResult = new ConjunctionMatchResult();
		conjunctionMatchResult.setNumConjuncts(numConjuncts);
		boolean isPresent = sufficientCoverage;
		for(int i = 1; i <= numConjuncts; i++) {
			BaseVariationScanner<?> conjunctScanner = conjunctScanners.get(i-1);
			Class<? extends VariationScannerMatchResult> conjunctMatchResultClass = conjunctScanner.getVariation().getVariationType().getMatchResultClass();
			isPresent &= updateConjunctionMatchResult(cmdContext, conjunctMatchResultClass, conjunctionMatchResult, conjunctScanner, i, queryToRefSegs, queryNts, qualityString);
		}
		return new VariationScanResult<ConjunctionMatchResult>(this, refStart, refEnd, sufficientCoverage, Arrays.asList(conjunctionMatchResult), isPresent);
	}
	
	private <D extends VariationScannerMatchResult> boolean updateConjunctionMatchResult(CommandContext cmdContext, Class<D> conjunctMatchResultClass, 
			ConjunctionMatchResult conjunctionMatchResult, BaseVariationScanner<?> conjunctScanner, int conjunctIndex,
			List<QueryAlignedSegment> queryToRefSegs, String queryNts, String qualityString) {
		@SuppressWarnings("unchecked")
		BaseVariationScanner<D> castConjunctScanner = (BaseVariationScanner<D>) conjunctScanner;
		VariationScanResult<D> conjunctScanResult = castConjunctScanner.scan(cmdContext, queryToRefSegs, queryNts, qualityString);
		conjunctionMatchResult.setConjunctResult(conjunctIndex, conjunctScanResult);
		Integer currentWorstQScore = conjunctionMatchResult.getWorstContributingQScore();
		Integer conjunctQScore = conjunctScanResult.getQScore();
		if(conjunctQScore != null) {
			if(currentWorstQScore == null || conjunctQScore < currentWorstQScore) {
				conjunctionMatchResult.setWorstContributingQScore(conjunctQScore);
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

	@Override
	public VariationScanResult<ConjunctionMatchResult> resolvePairedReadResults(
			SAMRecord record1,
			VariationScanResult<?> uncastVsr1,
			SAMRecord record2,
			VariationScanResult<?> uncastVsr2) {

		
		@SuppressWarnings("unchecked")
		VariationScanResult<ConjunctionMatchResult> vsr1 = (VariationScanResult<ConjunctionMatchResult>) uncastVsr1;
		@SuppressWarnings("unchecked")
		VariationScanResult<ConjunctionMatchResult> vsr2 = (VariationScanResult<ConjunctionMatchResult>) uncastVsr2;
		
		ConjunctionMatchResult result1 = vsr1.getVariationScannerMatchResults().get(0);
		ConjunctionMatchResult result2 = vsr2.getVariationScannerMatchResults().get(0);

		int readNameHashCoinFlip = Math.abs(record1.getReadName().hashCode()) % 2;
		ConjunctionMatchResult pairMatchResult = new ConjunctionMatchResult();
		
		boolean sufficientCoverage = true;
		boolean isPresent = true;
		
		for(int i = 1; i <= numConjuncts; i++) {
			VariationScanResult<?> conjunctResult1 = result1.getConjunctResult(i);
			VariationScanResult<?> conjunctResult2 = result2.getConjunctResult(i);
			Integer qScore1 = result1.getWorstContributingQScore();
			Integer qScore2 = result2.getWorstContributingQScore();
			Boolean sufficientCoverage1 = conjunctResult1.isSufficientCoverage();
			Boolean sufficientCoverage2 = conjunctResult2.isSufficientCoverage();
			if(sufficientCoverage1 && !sufficientCoverage2) {
				pairMatchResult.setConjunctResult(i, conjunctResult1);
			} else if(!sufficientCoverage1 && sufficientCoverage2) {
				pairMatchResult.setConjunctResult(i, conjunctResult2);
			} else if(qScore1 != null && qScore2 != null && qScore1 != qScore2) {
				if(qScore1 > qScore2) {
					pairMatchResult.setConjunctResult(i, conjunctResult1);
				} else {
					pairMatchResult.setConjunctResult(i, conjunctResult2);
				}
			} else if(record1.getMappingQuality() > record2.getMappingQuality()) {
				pairMatchResult.setConjunctResult(i, conjunctResult1);
			} else if(record1.getMappingQuality() < record2.getMappingQuality()) {
				pairMatchResult.setConjunctResult(i, conjunctResult2);
			} else if(readNameHashCoinFlip == 0) {
				pairMatchResult.setConjunctResult(i, conjunctResult1);
			} else {
				pairMatchResult.setConjunctResult(i, conjunctResult2);
			}
			
			Integer currentWorstQScore = pairMatchResult.getWorstContributingQScore();
			Integer conjunctQScore = pairMatchResult.getConjunctResult(i).getQScore();
			if(conjunctQScore != null) {
				if(currentWorstQScore == null || conjunctQScore < currentWorstQScore) {
					pairMatchResult.setWorstContributingQScore(conjunctQScore);
				}
			}
			sufficientCoverage &= pairMatchResult.getConjunctResult(i).isSufficientCoverage();
			isPresent &= pairMatchResult.getConjunctResult(i).isPresent();
		}

		return new VariationScanResult<ConjunctionMatchResult>(this,
				vsr1.getRefStart(), vsr1.getRefEnd(), 
				sufficientCoverage, Arrays.asList(pairMatchResult), isPresent);
		
	}

	
}
