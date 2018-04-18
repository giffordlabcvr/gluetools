package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.result.TableColumn;

public class ConjunctionMatchResult extends VariationScannerMatchResult {

	private Map<Integer, Class<? extends VariationScannerMatchResult>> conjunctIndexToResultClass = new LinkedHashMap<Integer, Class<? extends VariationScannerMatchResult>>();
	private Map<Integer, List<? extends VariationScannerMatchResult>> conjunctIndexToResults = new LinkedHashMap<Integer, List<? extends VariationScannerMatchResult>>();
	
	@Override
	public int getRefStart() {
		int refStart = Integer.MAX_VALUE;
		for(List<? extends VariationScannerMatchResult> conjunctResults: conjunctIndexToResults.values()) {
			for(VariationScannerMatchResult conjunctResult: conjunctResults) {
				int conjuctRefStart = conjunctResult.getRefStart();
				if(conjuctRefStart < refStart) {
					refStart = conjuctRefStart;
				}
			}
		}
		return refStart;
	}

	public <M extends VariationScannerMatchResult> void setConjunctResults(int conjunctIndex, 
			Class<M> matchResultClass, List<M> conjuctResults) {
		conjunctIndexToResultClass.put(conjunctIndex, matchResultClass);
		conjunctIndexToResults.put(conjunctIndex, conjuctResults);
	}
	
	public Class<? extends VariationScannerMatchResult> getConjunctResultClass(int conjunctIndex) {
		return conjunctIndexToResultClass.get(conjunctIndex);
	}

	public List<? extends VariationScannerMatchResult> getConjunctResults(int conjunctIndex) {
		return conjunctIndexToResults.get(conjunctIndex);
	}

	@SuppressWarnings("unchecked")
	public static List<TableColumn<ConjunctionMatchResult>> getTableColumns() {
		return Collections.emptyList();
	}
	
}
