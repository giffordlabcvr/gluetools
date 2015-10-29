package uk.ac.gla.cvr.gluetools.core.command.result;

import uk.ac.gla.cvr.gluetools.core.console.ConsoleOutputFormat;

public interface CommandResultRenderingContext {

	public void output(String outputLines);
	
	public ConsoleOutputFormat getConsoleOutputFormat();
}
