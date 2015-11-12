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

	public PreviewVariationsResult(List<Map<String, Object>> rowData) {
		super("previewVariationsResult", 
				Arrays.asList(VARIATION_NAME, REF_START, REF_END, REGEX, TRANSLATION_FORMAT), rowData);
	}

	protected PreviewVariationsResult(String rootObjectName,
			List<String> columnHeaders, List<Map<String, Object>> rowData) {
		super(rootObjectName, columnHeaders, rowData);
	}

	
	
	
}
