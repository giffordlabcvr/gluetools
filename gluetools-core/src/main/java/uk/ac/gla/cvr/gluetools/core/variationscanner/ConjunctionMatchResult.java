package uk.ac.gla.cvr.gluetools.core.variationscanner;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.result.TableColumn;
import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;

public class ConjunctionMatchResult extends VariationScannerMatchResult {

	private Map<Integer, VariationScanResult<?>> conjunctIndexToResult = new LinkedHashMap<Integer, VariationScanResult<?>>();

	@Override
	public int getRefStart() {
		int refStart = Integer.MAX_VALUE;
		for(VariationScanResult<?> conjunctResult: conjunctIndexToResult.values()) {
			for(VariationScannerMatchResult matchResult: conjunctResult.getVariationScannerMatchResults()) {
				int matchRefStart = matchResult.getRefStart();
				if(matchRefStart < refStart) {
					refStart = matchRefStart;
				}
			}
		}
		return refStart;
	}

	public <M extends VariationScannerMatchResult> void setConjunctResults(int conjunctIndex, VariationScanResult<?> result) {
		conjunctIndexToResult.put(conjunctIndex, result);
	}
	
	@SuppressWarnings("unchecked")
	public static List<TableColumn<ConjunctionMatchResult>> getTableColumns() {
		return Collections.emptyList();
	}

	@Override
	public <M extends VariationScannerMatchResult> void populateMatchObject(CommandObject matchObject) {
		CommandArray conjunctArray = matchObject.setArray("conjuncts");
		conjunctIndexToResult.forEach((conjunctIndex, vsr) -> {
			CommandObject variationObject = conjunctArray.addObject();
			variationObject.setInt("conjunctIndex", conjunctIndex);
			VariationScanResult.variationScanResultAsCommandObject(variationObject, vsr);
		});

	}
	
}
