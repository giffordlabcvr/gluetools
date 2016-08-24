package uk.ac.gla.cvr.gluetools.core.reporting.fastaSequenceReporter;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanResult;

public class FastaSequenceVariationScanResult extends BaseTableResult<VariationScanResult> {

	public static final String 
		REF_SEQ_NAME = "referenceName",
		FEATURE_NAME = "featureName",
		VARIATION_NAME = "variationName",
		PRESENT = "present",
		QUERY_NT_START = "queryNtStart",
		QUERY_NT_END = "queryNtEnd";


	public FastaSequenceVariationScanResult(List<VariationScanResult> rowData) {
		super("fastaSequenceVariationScanResult", 
				rowData, 
				column(REF_SEQ_NAME, vsr -> vsr.getVariation().getFeatureLoc().getReferenceSequence().getName()),
				column(FEATURE_NAME, vsr -> vsr.getVariation().getFeatureLoc().getFeature().getName()),
				column(VARIATION_NAME, vsr -> vsr.getVariation().getName()),
				column(PRESENT, vsr -> vsr.isPresent()),
				column(QUERY_NT_START, vsr -> vsr.getQueryNtStart()), 
				column(QUERY_NT_END, vsr -> vsr.getQueryNtEnd()));
	}

}
