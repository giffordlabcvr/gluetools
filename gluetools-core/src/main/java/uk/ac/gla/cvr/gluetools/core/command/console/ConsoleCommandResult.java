package uk.ac.gla.cvr.gluetools.core.command.console;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResultRenderingContext;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public class ConsoleCommandResult extends CommandResult {

	public ConsoleCommandResult(String text) {
		super("consoleCommandResult");
		getDocumentBuilder().set("resultText", text);
	}
	
	@Override
	protected final void renderToConsoleAsText(CommandResultRenderingContext renderCtx) {
		renderCtx.output(GlueXmlUtils.getXPathString(getDocument(), "/consoleCommandResult/resultText/text()"));
	}
	
}
