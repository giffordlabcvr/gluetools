package uk.ac.gla.cvr.gluetools.core.command.console.config;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;


@CommandClass( 
		commandWords = {"console","unset"},
		docoptUsages = {"<optionName>"}, 
		modeWrappable = false,
		description = "Unset a console option's value",
		furtherHelp = "After unsetting the option, its default value will be in effect")
public class ConsoleUnsetOptionCommand extends ConsoleOptionCommand<OkResult> {
	
	@Override
	protected OkResult executeOnConsole(ConsoleCommandContext cmdContext) {
		cmdContext.unsetOptionValue(getConsoleOption());
		return new OkResult();
	}

	@CompleterClass
	public static class Completer extends OptionNameCompleter {}

}