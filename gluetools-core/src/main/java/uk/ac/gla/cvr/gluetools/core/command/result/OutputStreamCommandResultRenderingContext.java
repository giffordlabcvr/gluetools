package uk.ac.gla.cvr.gluetools.core.command.result;

import java.io.OutputStream;
import java.io.PrintWriter;

import uk.ac.gla.cvr.gluetools.utils.FastaUtils.LineFeedStyle;

public class OutputStreamCommandResultRenderingContext implements CommandResultRenderingContext {
	private PrintWriter printWriter;
	private ResultOutputFormat consoleOutputFormat;
	private LineFeedStyle lineFeedStyle;
	public OutputStreamCommandResultRenderingContext(OutputStream outputStream, ResultOutputFormat consoleOutputFormat,
			LineFeedStyle lineFeedStyle) {
		this.printWriter = new PrintWriter(outputStream);
		this.consoleOutputFormat = consoleOutputFormat;
		this.lineFeedStyle = lineFeedStyle;
	}
	
	@Override
	public void output(String message) {
		output(message, true);
	}

	@Override
	public void output(String message, boolean newLine) {
		printWriter.print(message);
		if(newLine) {
			printWriter.print(lineFeedStyle.getLineBreakChars());
		}
		printWriter.flush();
	}

	@Override
	public ResultOutputFormat getResultOutputFormat() {
		return this.consoleOutputFormat;
	}

	
	
}