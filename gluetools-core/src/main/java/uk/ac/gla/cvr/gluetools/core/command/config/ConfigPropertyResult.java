package uk.ac.gla.cvr.gluetools.core.command.config;

import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;

public class ConfigPropertyResult extends MapResult {

	public ConfigPropertyResult(String propertyName, String propertyValue) {
		super("configPropertyResult", mapBuilder().put(propertyName, propertyValue));
	}

}
