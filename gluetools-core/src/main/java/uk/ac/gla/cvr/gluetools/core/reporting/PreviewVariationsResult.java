package uk.ac.gla.cvr.gluetools.core.reporting;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;

public class PreviewVariationsResult extends TableResult {

	public static final String VARIATION_NAME = "variationName";
	public static final String REF_START = "refStart";
	public static final String REF_END = "refEnd";
	public static final String REGEX = "regex";
	public static final String TRANSLATION_FORMAT = "translationFormat";
	public static final String VARIATION_CATEGORIES = "variationCategories";

	public PreviewVariationsResult(List<Map<String, Object>> rowData) {
		super("generateVariationsResult", 
				Arrays.asList(VARIATION_NAME, REF_START, REF_END, REGEX, TRANSLATION_FORMAT, VARIATION_CATEGORIES), rowData);
	}

	
	
}
