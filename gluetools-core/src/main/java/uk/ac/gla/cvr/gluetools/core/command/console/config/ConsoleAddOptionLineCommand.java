package uk.ac.gla.cvr.gluetools.core.command.console.config;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;


@CommandClass( 
		commandWords = {"console","add","option-line"},
		docoptUsages = {"<optionName>"}, 
		metaTags = { CmdMeta.consoleOnly, CmdMeta.nonModeWrappable },
		description = "Add an option line to the console display")
public class ConsoleAddOptionLineCommand extends ConsoleOptionCommand<OkResult> {
	
	@Override
	public OkResult execute(CommandContext cmdContext) {
		ConsoleCommandContext consoleCommandContext = (ConsoleCommandContext) cmdContext;
		consoleCommandContext.addOptionLine(getConsoleOption());
		return new OkResult();
	}
	
	@CompleterClass
	public static class Completer extends OptionNameCompleter {
	}


}