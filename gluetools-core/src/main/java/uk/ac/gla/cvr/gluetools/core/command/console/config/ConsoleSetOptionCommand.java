package uk.ac.gla.cvr.gluetools.core.command.console.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.ConsoleOption;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.console.config.ConsoleOptionException.Code;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
		commandWords = {"console","set"},
		docoptUsages = {"<optionName> <optionValue>"}, 
		modeWrappable = false,
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
	protected OkResult executeOnConsole(ConsoleCommandContext cmdContext) {
		cmdContext.setOptionValue(getConsoleOption(), optionValue);
		return new OkResult();
	}
	
	@CompleterClass
	@SuppressWarnings("rawtypes")
	public static class Completer extends OptionNameCompleter {

		@Override
		public List<String> completionSuggestions(
				ConsoleCommandContext cmdContext,
				Class<? extends Command> cmdClass, List<String> argStrings) {
			if(argStrings.size() == 1) {
				String optionName = argStrings.get(0);
				try {
					ConsoleOption consoleOption = lookupOptionByName(optionName);
					String[] allowedValues = consoleOption.getAllowedValues();
					if(allowedValues != null) {
						return Arrays.asList(allowedValues);
					}
				} catch(ConsoleOptionException coe) {
					// bad option name.
				}
				return new ArrayList<String>();
			}
			return super.completionSuggestions(cmdContext, cmdClass, argStrings);
		}
		
	}


}