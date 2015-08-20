package uk.ac.gla.cvr.gluetools.core.command.console.config;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.ConsoleOption;
import uk.ac.gla.cvr.gluetools.core.command.console.SimpleConsoleCommandResult;


@CommandClass( 
		commandWords = {"console","help"},
		docoptUsages = {"<optionName>"}, 
		metaTags = { CmdMeta.nonModeWrappable },
		description = "Show help for a specific console option")
public class ConsoleHelpOptionCommand extends ConsoleOptionCommand<SimpleConsoleCommandResult> {

	@Override
	public SimpleConsoleCommandResult execute(CommandContext cmdContext) {
		ConsoleOption consoleOption = getConsoleOption();
		return new SimpleConsoleCommandResult(consoleOption.getName()+": "+consoleOption.getDescription());
	}
	
	@CompleterClass
	public static class Completer extends OptionNameCompleter {}

}