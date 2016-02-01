package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;

public class SamVariationsScanResult extends TableResult {

	public static final String 
		VARIATION_NAME = "variationName",
		READS_CONFIRMED_PRESENT = "readsConfirmedPresent",
		READS_CONFIRMED_ABSENT = "readsConfirmedAbsent";
	
	
	public SamVariationsScanResult(List<Map<String, Object>> rowData) {
		super("samVariationsScanResult", 
				Arrays.asList(
						VARIATION_NAME, 
						READS_CONFIRMED_PRESENT, READS_CONFIRMED_ABSENT), 
				rowData);
	}

}
