package uk.ac.gla.cvr.gluetools.core.digs.importer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;

public class SynchroniseFieldsExtractedResult extends TableResult {

	public static final String STATUS = "status";
	public static final String GLUE_FIELD = "glueField";
	public static final String DIGS_FIELD = "digsField";
	public static final String GLUE_TYPE = "type";
	public static final String GLUE_LENGTH = "length";

	public SynchroniseFieldsExtractedResult(List<Map<String, Object>> rowData) {
		super("synchronizeFieldsResult", Arrays.asList(DIGS_FIELD, GLUE_FIELD, GLUE_TYPE, GLUE_LENGTH, STATUS), rowData);
	}


}
