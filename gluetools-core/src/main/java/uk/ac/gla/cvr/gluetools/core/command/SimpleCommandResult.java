package uk.ac.gla.cvr.gluetools.core.command;

import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandResult;


public class SimpleCommandResult extends ConsoleCommandResult {

	private String commandLineResponse;
	
	public SimpleCommandResult(String commandLineResponse) {
		this.commandLineResponse = commandLineResponse;
	}

	@Override
	public String getResultAsConsoleText() {
		return commandLineResponse;
	}

}
