package uk.ac.gla.cvr.gluetools.core.command.console.config;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.ConsoleOption;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.config.ConsoleOptionException.Code;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class ConsoleOptionCommand<R extends CommandResult> extends Command<R> {

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
	
	public abstract static class OptionNameCompleter extends AdvancedCmdCompleter {
		public OptionNameCompleter() {
			super();
			registerVariableInstantiator("optionName", new VariableInstantiator() {
				@Override
				@SuppressWarnings("rawtypes")
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					return Arrays.asList(ConsoleOption.values())
							.stream()
							.map(co -> new CompletionSuggestion(co.getName(), true))
							.collect(Collectors.toList());
				}
			});
		}
	}
	
	
}
