package uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanRenderHints;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanResult;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanResultRow;

public class FastaSequenceVariationScanResult extends BaseTableResult<VariationScanResultRow> {

	public static final String 
		REF_SEQ_NAME = "referenceName",
		FEATURE_NAME = "featureName",
		VARIATION_NAME = "variationName";

	public FastaSequenceVariationScanResult(VariationScanRenderHints renderHints, List<VariationScanResult> rowData) {
		super("fastaSequenceVariationScanResult", 
				renderHints.scanResultsToResultRows(rowData), 
				renderHints.generateResultColumns(
					column(REF_SEQ_NAME, vsrr -> vsrr.getVariationReferenceName()),
					column(FEATURE_NAME, vsrr -> vsrr.getVariationFeatureName()),
					column(VARIATION_NAME, vsrr -> vsrr.getVariationName())
				));
	}

}
