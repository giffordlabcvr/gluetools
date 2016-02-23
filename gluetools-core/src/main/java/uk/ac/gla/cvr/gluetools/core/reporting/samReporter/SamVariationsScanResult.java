package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;

public class SamVariationsScanResult extends TableResult {

	public static final String 
		VARIATION_NAME = "variationName",
		READS_PRESENT = "readsPresent",
		PCT_PRESENT = "pctPresent",
		READS_ABSENT = "readsAbsent",
		PCT_ABSENT = "pctAbsent";
	
	
	public SamVariationsScanResult(List<Map<String, Object>> rowData) {
		super("samVariationsScanResult", 
				Arrays.asList(
						VARIATION_NAME, 
						READS_PRESENT, 
						PCT_PRESENT, 
						READS_ABSENT,
						PCT_ABSENT), 
				rowData);
	}

}
