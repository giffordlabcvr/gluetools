package uk.ac.gla.cvr.gluetools.core.command.console.config;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.ConsoleOption;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.SimpleConsoleCommandResult;


@CommandClass( 
		commandWords = {"console","help"},
		docoptUsages = {"<optionName>"}, 
		modeWrappable = false,
		description = "Show help for a specific console option")
public class ConsoleHelpOptionCommand extends ConsoleOptionCommand<SimpleConsoleCommandResult> {

	@Override
	protected SimpleConsoleCommandResult executeOnConsole(ConsoleCommandContext cmdContext) {
		ConsoleOption consoleOption = getConsoleOption();
		return new SimpleConsoleCommandResult(consoleOption.getName()+": "+consoleOption.getDescription());
	}
	
	@CompleterClass
	public static class Completer extends OptionNameCompleter {}

}