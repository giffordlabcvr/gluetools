package uk.ac.gla.cvr.gluetools.core.command.console;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResultRenderingContext;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;

public class ConsoleCommandResult extends MapResult {

	public ConsoleCommandResult(String text) {
		super("consoleCommandResult", mapBuilder().put("resultText", text));
	}
	
	@Override
	protected final void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		renderCtx.output(getDocumentReader().stringValue("resultText"));
	}
	
}
