package uk.ac.gla.cvr.gluetools.core.command.result;

import java.io.OutputStream;
import java.io.PrintWriter;

public class OutputStreamCommandResultRenderingContext implements CommandResultRenderingContext {
	private PrintWriter printWriter;
	private ResultOutputFormat consoleOutputFormat;
	public OutputStreamCommandResultRenderingContext(OutputStream outputStream, ResultOutputFormat consoleOutputFormat) {
		this.printWriter = new PrintWriter(outputStream);
		this.consoleOutputFormat = consoleOutputFormat;
	}
	
	@Override
	public void output(String message) {
		output(message, true);
	}

	@Override
	public void output(String message, boolean newLine) {
		if(newLine) {
			printWriter.println(message);
		} else {
			printWriter.print(message);
		}
		printWriter.flush();
	}

	@Override
	public ResultOutputFormat getResultOutputFormat() {
		return this.consoleOutputFormat;
	}
	
}