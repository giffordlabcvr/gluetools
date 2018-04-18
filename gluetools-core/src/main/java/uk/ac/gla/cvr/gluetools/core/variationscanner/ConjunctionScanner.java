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
			conjunctScanners.add(conjunctVariation_i.getScanner(cmdContext));
		}
		
	}
	
	@Override
	public void validate() {
		super.validate();
		int lastNonNullConjunct = 1;
		for(int i = 2; i <= MAX_CONJUNCT_INDEX; i++) {
			String conjunctName_i = getStringMetatagValue(VariationMetatagType.valueOf("CONJUNCT_NAME_"+i));
			if(conjunctName_i != null && lastNonNullConjunct < i - 1) {
				throwScannerException("Conjunction variation should use consecutive conjunct indices");
			}
			lastNonNullConjunct = i;
		}
		for(int i = 1; i <= MAX_CONJUNCT_INDEX; i++) {
			try {
				conjunctScanners.get(i).validate();
			} catch(GlueException ge) {
				throwScannerException(ge, "Validation of conjunct "+i+" failed: "+ge.getLocalizedMessage());
			}
		}
		
	}

	public int getNumConjucts() {
		return numConjuncts;
	}

	protected boolean computeSufficientCoverage(List<NtQueryAlignedSegment> queryToRefNtSegs) {
		for(int i = 1; i <= numConjuncts; i++) {
			boolean conjunctResult = conjunctScanners.get(i).computeSufficientCoverage(queryToRefNtSegs);
			if(!conjunctResult) {
				return false;
			}
		}
		return true;
	}

	@Override
	protected VariationScanResult<ConjunctionMatchResult> scanInternal(
			CommandContext cmdContext,
			List<NtQueryAlignedSegment> queryToRefNtSegs, String queryNts) {
		boolean sufficientCoverage = computeSufficientCoverage(queryToRefNtSegs);
		if(!sufficientCoverage) {
			return new VariationScanResult<ConjunctionMatchResult>(getVariation(), sufficientCoverage, Collections.emptyList());
		}
		ConjunctionMatchResult conjunctionMatchResult = new ConjunctionMatchResult();
		boolean isPresent = true;
		for(int i = 1; i <= numConjuncts; i++) {
			BaseVariationScanner<?> conjunctScanner = conjunctScanners.get(i);
			Class<? extends VariationScannerMatchResult> conjunctMatchResultClass = conjunctScanner.getVariation().getVariationType().getMatchResultClass();
			isPresent &= updateConjunctionMatchResult(conjunctMatchResultClass, conjunctionMatchResult, conjunctScanner, i, cmdContext, queryToRefNtSegs, queryNts);
		}
		if(isPresent) {
			return new VariationScanResult<ConjunctionMatchResult>(getVariation(), sufficientCoverage, Arrays.asList(conjunctionMatchResult));
		} else {
			return new VariationScanResult<ConjunctionMatchResult>(getVariation(), sufficientCoverage, Collections.emptyList());
		}
	}
	
	private <D extends VariationScannerMatchResult> boolean updateConjunctionMatchResult(Class<D> conjunctMatchResultClass, 
			ConjunctionMatchResult conjunctionMatchResult, BaseVariationScanner<?> conjunctScanner, int conjunctIndex, CommandContext cmdContext,
			List<NtQueryAlignedSegment> queryToRefNtSegs, String queryNts) {
		@SuppressWarnings("unchecked")
		BaseVariationScanner<D> castConjunctScanner = (BaseVariationScanner<D>) conjunctScanner;
		VariationScanResult<D> conjunctScanResult = castConjunctScanner.scan(cmdContext, queryToRefNtSegs, queryNts);
		List<D> conjunctMatchResults = conjunctScanResult.getVariationScannerMatchResults();
		conjunctionMatchResult.setConjunctResults(conjunctIndex, conjunctMatchResultClass, conjunctMatchResults);
		return conjunctScanResult.isPresent();
	}
	
}
