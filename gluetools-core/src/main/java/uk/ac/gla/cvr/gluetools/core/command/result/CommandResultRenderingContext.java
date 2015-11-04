package uk.ac.gla.cvr.gluetools.core.command.result;

import java.io.InputStream;

import uk.ac.gla.cvr.gluetools.core.console.ConsoleOutputFormat;

public interface CommandResultRenderingContext {

	public void output(String outputLines);
	
	public void output(String outputLines, boolean newLine);

	public ConsoleOutputFormat getConsoleOutputFormat();
	
	public int getTerminalWidth();
	
	public int getTerminalHeight();

	public InputStream getInputStream();
}
