package uk.ac.gla.cvr.gluetools.core.http;

import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;

public class HttpRunnerResult extends MapResult {

	public HttpRunnerResult(String entityAsString) {
		super("httpRunnerResult", mapBuilder()
				.put("entityAsString", entityAsString));
	}
}
