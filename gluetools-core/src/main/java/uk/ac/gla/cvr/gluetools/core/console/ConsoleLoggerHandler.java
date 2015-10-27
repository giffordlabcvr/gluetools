package uk.ac.gla.cvr.gluetools.core.console;

import java.util.logging.ErrorManager;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class ConsoleLoggerHandler extends Handler {
	private Console console;

	public ConsoleLoggerHandler(Console console) {
		super();
		this.console = console;
	}

	@Override
	public void publish(LogRecord record) {
		String msg;
        try {
            msg = getFormatter().format(record);
        } catch (Exception ex) {
            // We don't want to throw an exception here, but we
            // report the exception to any registered ErrorManager.
            reportError(null, ex, ErrorManager.FORMAT_FAILURE);
            return;
        }
        console.output(msg);
	}

	@Override
	public void flush() {
	}

	@Override
	public void close() throws SecurityException {
	}
}
