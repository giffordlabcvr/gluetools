package uk.ac.gla.cvr.gluetools.core.collation.populating.textfile;

import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

public class TextFilePopulatorResult extends BaseTableResult<Map<String,String>> {

	public static final String SOURCE_NAME = "sourceName";
	public static final String SEQUENCE_ID = "sequenceID";
	public static final String FIELD_NAME = "fieldName";
	public static final String FIELD_VALUE = "fieldValue";

	public TextFilePopulatorResult(List<Map<String, String>> rowObjects) {
		super("textFilePopulatorResult", rowObjects, column(SOURCE_NAME, x -> x.get(SOURCE_NAME)), 
				column(SEQUENCE_ID, x -> x.get(SEQUENCE_ID)), 
				column(FIELD_NAME, x -> x.get(FIELD_NAME)), 
				column(FIELD_VALUE, x -> x.get(FIELD_VALUE)));
	}

}
