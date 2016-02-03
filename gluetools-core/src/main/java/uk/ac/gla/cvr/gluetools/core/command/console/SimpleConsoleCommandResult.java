package uk.ac.gla.cvr.gluetools.core.command.console;

public class SimpleConsoleCommandResult extends ConsoleCommandResult {

	public SimpleConsoleCommandResult(String commandLineResponse, boolean wrap) {
		super(commandLineResponse, wrap);
	}

	public SimpleConsoleCommandResult(String commandLineResponse) {
		super(commandLineResponse);
	}

}
