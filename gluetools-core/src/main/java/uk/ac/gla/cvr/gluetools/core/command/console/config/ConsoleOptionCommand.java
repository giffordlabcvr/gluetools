package uk.ac.gla.cvr.gluetools.core.command.console.config;

import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandCompleter;
import uk.ac.gla.cvr.gluetools.core.command.ConsoleOption;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.config.ConsoleOptionException.Code;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class ConsoleOptionCommand<R extends CommandResult> extends ConsoleCommand<R> {

	private ConsoleOption consoleOption;

	protected ConsoleOption getConsoleOption() {
		return consoleOption;
	}
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		String optionName = PluginUtils.configureStringProperty(configElem, "optionName", true);
		consoleOption = lookupOptionByName(optionName);
	}

	
	protected static ConsoleOption lookupOptionByName(String optionName) {
		for(ConsoleOption option : ConsoleOption.values()) {
			if(option.getName().equals(optionName)) {
				return option;
			}
		}
		throw new ConsoleOptionException(Code.NO_SUCH_OPTION, optionName);
	}
	
	@SuppressWarnings("rawtypes")
	public abstract static class OptionNameCompleter extends CommandCompleter {
		@Override
		public List<String> completionSuggestions(ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass, List<String> argStrings) {
			if(argStrings.isEmpty()) {
				LinkedList<String> suggestions = new LinkedList<String>();
				for(ConsoleOption consoleOption : ConsoleOption.values()) {
					suggestions.add(consoleOption.getName());
				}
				return suggestions;
			}
			return super.completionSuggestions(cmdContext, cmdClass, argStrings);
		}
	}
	
	
}
