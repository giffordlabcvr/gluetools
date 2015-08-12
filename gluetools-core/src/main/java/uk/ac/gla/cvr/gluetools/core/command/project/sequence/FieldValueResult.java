package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;

public class FieldValueResult extends MapResult {

	public FieldValueResult(String fieldName, String fieldValue) {
		super("fieldValueResult", mapBuilder()
				.put("fieldName", fieldName)
				.put("value", fieldValue));
	}
	
}
