package uk.ac.gla.cvr.gluetools.core.command.console;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandResult;
import uk.ac.gla.cvr.gluetools.core.console.ConsoleException;
import uk.ac.gla.cvr.gluetools.core.console.ConsoleException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass(
	commandWords={"help"},
	docoptUsages={"[<commandWord> ...]"},
	description="Print help about available commands beginning with certain words"
) 
public class HelpCommand extends ConsoleCommand {

	private List<String> commandWords;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		commandWords = PluginUtils.configureStringsProperty(configElem, "commandWords");
	}

	
	@Override
	protected CommandResult executeOnConsole(ConsoleCommandContext cmdContext) {
		CommandFactory commandFactory = cmdContext.peekCommandMode().getCommandFactory();
		/*
		if(!commandWords.isEmpty()) {
			Class<? extends Command> cmdClass = commandFactory.classForElementName(command);
			if(cmdClass == null) {
				throw new ConsoleException(Code.UNKNOWN_COMMAND, command, cmdContext.getModePath());
			} else {
				return new HelpSpecificCommandResult(cmdClass);
			}
		} else {
			List<Class<? extends Command>> cmdClasses = commandFactory.getRegisteredClasses();
			return new HelpCommandResult(cmdClasses);
		}*/
		return CommandResult.OK;
	}

	

	
}
