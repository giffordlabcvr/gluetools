package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class SamVariationScanResult extends BaseTableResult<VariationScanReadCount> {

	public static final String 
		VARIATION_NAME = "variationName",
		READS_PRESENT = "readsPresent",
		PCT_PRESENT = "pctPresent",
		READS_ABSENT = "readsAbsent",
		PCT_ABSENT = "pctAbsent";
	
	
	public SamVariationScanResult(List<VariationScanReadCount> rowData) {
		super("samVariationsScanResult", 
				rowData,
				column(VARIATION_NAME, vsrc -> vsrc.getVariationName()), 
				column(READS_PRESENT, vsrc -> vsrc.getReadsWherePresent()), 
				column(PCT_PRESENT, vsrc -> vsrc.getPctWherePresent()), 
				column(READS_ABSENT, vsrc -> vsrc.getReadsWhereAbsent()),
				column(PCT_ABSENT, vsrc -> vsrc.getPctWhereAbsent()));
	}

}
