package uk.ac.gla.cvr.gluetools.core.command.console.help;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.console.ConsoleException;
import uk.ac.gla.cvr.gluetools.core.console.ConsoleException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass(
	commandWords={"help"},
	docoptUsages={"[<commandWord> ...]"},
	description="Command help, based on a word sequence"
) 
public class HelpCommand extends ConsoleCommand {

	private List<String> commandWords;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		commandWords = PluginUtils.configureStringsProperty(configElem, "commandWord");
	}

	
	@Override
	protected CommandResult executeOnConsole(ConsoleCommandContext cmdContext) {
		CommandFactory commandFactory = cmdContext.peekCommandMode().getCommandFactory();
		List<HelpLine> helpLines = commandFactory.helpLinesForCommandWords(cmdContext, commandWords);
		if(helpLines.isEmpty()) {
			throw new ConsoleException(Code.UNKNOWN_COMMAND, String.join(" ", commandWords), cmdContext.getModePath());
		} else if(helpLines.size() == 1 && helpLines.get(0) instanceof SpecificCommandHelpLine) {
			return new HelpSpecificCommandResult(((SpecificCommandHelpLine) helpLines.get(0)).getCmdClass());
		} else {
			return new HelpCommandResult(helpLines);
		}
	}

	@CompleterClass
	public static class HelpCommandCompleter extends CommandCompleter {
		@Override
		public List<String> completionSuggestions(ConsoleCommandContext commandContext, List<String> argStrings) {
			CommandFactory commandFactory = commandContext.peekCommandMode().getCommandFactory();
			return commandFactory.getCommandWordSuggestions(commandContext, argStrings, false, commandContext.isRequireModeWrappable());
		}
		
	}

	
}
