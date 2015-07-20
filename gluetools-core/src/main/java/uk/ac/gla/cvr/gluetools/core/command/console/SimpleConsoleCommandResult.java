package uk.ac.gla.cvr.gluetools.core.command.console;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResultRenderingContext;



public class SimpleConsoleCommandResult extends ConsoleCommandResult {

	private String commandLineResponse;
	
	public SimpleConsoleCommandResult(String commandLineResponse) {
		this.commandLineResponse = commandLineResponse;
	}

	@Override
	public void renderToConsole(CommandResultRenderingContext renderCtx) {
		renderCtx.output(commandLineResponse);
	}

}
