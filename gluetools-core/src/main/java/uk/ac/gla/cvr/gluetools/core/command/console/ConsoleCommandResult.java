package uk.ac.gla.cvr.gluetools.core.command.console;

import org.apache.commons.lang3.text.WordUtils;

import uk.ac.gla.cvr.gluetools.core.command.result.InteractiveCommandResultRenderingContext;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;

public class ConsoleCommandResult extends MapResult {

	private boolean wrap;

	public ConsoleCommandResult(String text) {
		this(text, false);
	}
	
	public ConsoleCommandResult(String text, boolean wrap) {
		super("consoleCommandResult", mapBuilder().put("resultText", text));
		this.wrap = wrap;
	}
	
	@Override
	protected void renderToConsoleAsText(InteractiveCommandResultRenderingContext renderCtx) {
		String text = getCommandDocument().getString("resultText");
		if(wrap) {
			int terminalWidth = renderCtx.getTerminalWidth();
			renderCtx.output(WordUtils.wrap(text, terminalWidth, "\n", false));
		} else {
			renderCtx.output(text);
		}
	}
	
}
