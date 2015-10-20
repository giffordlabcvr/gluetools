package uk.ac.gla.cvr.gluetools.core.command.console.config;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.ConsoleOption;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.config.ConsoleOptionException.Code;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
		commandWords = {"console","set"},
		docoptUsages = {"<optionName> <optionValue>"}, 
		metaTags = { CmdMeta.consoleOnly, CmdMeta.nonModeWrappable },
		description = "Set a console option's value")
public class ConsoleSetOptionCommand extends ConsoleOptionCommand<OkResult> {
	
	private String optionValue;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		optionValue = PluginUtils.configureStringProperty(configElem, "optionValue", true);
		ConsoleOption consoleOption = getConsoleOption();
		String[] allowedValues = consoleOption.getAllowedValues();
		if(allowedValues != null) {
			List<String> valuesList = Arrays.asList(allowedValues);
			if(!valuesList.contains(optionValue)) {
				throw new ConsoleOptionException(Code.INVALID_OPTION_VALUE, consoleOption.getName(), optionValue, valuesList.toString());
			}
		}
	}
	
	@Override
	public OkResult execute(CommandContext cmdContext) {
		ConsoleCommandContext consoleCommandContext = (ConsoleCommandContext) cmdContext;
		consoleCommandContext.setOptionValue(getConsoleOption(), optionValue);
		return new OkResult();
	}
	
	@CompleterClass
	public static class Completer extends OptionNameCompleter {

		public Completer() {
			super();
			registerVariableInstantiator("optionValue", new VariableInstantiator() {
				@Override
				@SuppressWarnings("rawtypes")
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					try {
						ConsoleOption consoleOption = ConsoleOptionCommand.lookupOptionByName((String) bindings.get("optionName"));
						String[] allowedValues = consoleOption.getAllowedValues();
						if(allowedValues != null) {
							return Arrays.asList(allowedValues).stream().map(s -> new CompletionSuggestion(s, true)).collect(Collectors.toList());
						}
					} catch(ConsoleOptionException coe) {
						// bad option name.
					}
					return null;
				}
			});
			}

	}


}