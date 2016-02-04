package uk.ac.gla.cvr.gluetools.core.digs.importer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.result.TableResult;

public class ImportExtractedResult extends TableResult {

	public static final String BLAST_ID = "blastID";
	public static final String SEQUENCE = "sequence";

	public ImportExtractedResult(List<Map<String, Object>> rowData) {
		super("importHitsResult", Arrays.asList(BLAST_ID, SEQUENCE), rowData);
	}

}
