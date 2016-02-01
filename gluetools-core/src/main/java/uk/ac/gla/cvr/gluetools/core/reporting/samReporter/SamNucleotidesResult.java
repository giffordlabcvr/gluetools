package uk.ac.gla.cvr.gluetools.core.reporting.samReporter;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;

public class SamNucleotidesResult extends TableResult {

	public static final String 
		GLUE_REFERENCE_NT = "glueReferenceNt",
		SAM_REFERENCE_NT = "samReferenceNt",
		READS_WITH_A = "readsWithA",
		READS_WITH_C = "readsWithC",
		READS_WITH_G = "readsWithG",
		READS_WITH_T = "readsWithT";


	public SamNucleotidesResult(List<Map<String, Object>> rowData) {
		super("samNucleotidesResult", 
				Arrays.asList(
						SAM_REFERENCE_NT, GLUE_REFERENCE_NT, 
						READS_WITH_A, READS_WITH_C, READS_WITH_G, READS_WITH_T), 
				rowData);
	}

}
