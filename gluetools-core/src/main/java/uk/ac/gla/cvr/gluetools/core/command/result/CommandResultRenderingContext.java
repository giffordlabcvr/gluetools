package uk.ac.gla.cvr.gluetools.core.command.result;

import uk.ac.gla.cvr.gluetools.core.console.ConsoleOutputFormat;
import uk.ac.gla.cvr.gluetools.utils.RenderContext;

public interface CommandResultRenderingContext extends RenderContext {

	public void output(String outputLines);
	
	public void output(String outputLines, boolean newLine);

	public ConsoleOutputFormat getConsoleOutputFormat();
	
}
