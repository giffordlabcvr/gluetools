package uk.ac.gla.cvr.gluetools.core.reporting;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class GenerateVariationsResult extends PreviewVariationsResult {

	public static final String MERGE = "merge";
	public static final String VARIATION_CATEGORIES = "variationCategories";

	public GenerateVariationsResult(List<Map<String, Object>> rowData) {
		super("generateCommonVariationsResult", 
				Arrays.asList(VARIATION_NAME, REF_START, REF_END, REGEX, TRANSLATION_FORMAT, MERGE, VARIATION_CATEGORIES), rowData);
	}

	
	
}
