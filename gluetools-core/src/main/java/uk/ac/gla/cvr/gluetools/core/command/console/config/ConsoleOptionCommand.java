/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.command.console.config;

import java.util.ArrayList;
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

	
	public static ConsoleOption lookupOptionByName(String optionName) {
		List<String> validOptions = new ArrayList<String>();
		for(ConsoleOption option : ConsoleOption.values()) {
			if(option.getName().equals(optionName)) {
				return option;
			}
			validOptions.add(option.getName());
		}
		throw new ConsoleOptionException(Code.NO_SUCH_OPTION, optionName, validOptions);
	}
	
	public abstract static class OptionNameCompleter extends AdvancedCmdCompleter {
		public OptionNameCompleter() {
			super();
			registerVariableInstantiator("optionName", new VariableInstantiator() {
				@Override
				@SuppressWarnings("rawtypes")
				public List<CompletionSuggestion> instantiate(
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
