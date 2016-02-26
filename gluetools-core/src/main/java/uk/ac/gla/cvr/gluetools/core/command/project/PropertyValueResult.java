package uk.ac.gla.cvr.gluetools.core.command.project;

import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;

public class PropertyValueResult extends MapResult {

	public PropertyValueResult(String property, String value) {
		super("propertyValueResult", mapBuilder()
				.put("property", property)
				.put("value", value));
	}
	
}
