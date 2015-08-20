package uk.ac.gla.cvr.gluetools.core.command.console.config;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.ConsoleOption;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.SimpleConsoleCommandResult;


@CommandClass( 
		commandWords = {"console","show"},
		docoptUsages = {"<optionName>"}, 
		metaTags = { CmdMeta.consoleOnly, CmdMeta.nonModeWrappable },
		description = "Show the current value of a console option")
public class ConsoleShowOptionCommand extends ConsoleOptionCommand<SimpleConsoleCommandResult> {

	@Override
	public SimpleConsoleCommandResult execute(CommandContext cmdContext) {
		ConsoleCommandContext consoleCommandContext = (ConsoleCommandContext) cmdContext;
		ConsoleOption consoleOption = getConsoleOption();
		String valueText = consoleCommandContext.getConfiguredOptionValue(consoleOption);
		if(valueText == null) {
			valueText = consoleOption.getDefaultValue()+" (default value)";
		}
		return new SimpleConsoleCommandResult(valueText);
	}

	@CompleterClass
	public static class Completer extends OptionNameCompleter {}

}