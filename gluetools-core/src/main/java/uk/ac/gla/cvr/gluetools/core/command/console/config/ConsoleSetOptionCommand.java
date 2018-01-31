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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
				throw new ConsoleOptionException(Code.INVALID_OPTION_VALUE, consoleOption.getName(), optionValue, "Valid values: "+valuesList.toString());
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
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					try {
						ConsoleOption consoleOption = ConsoleOptionCommand.lookupOptionByName((String) bindings.get("optionName"));
						return consoleOption.instantiateValue(cmdContext, cmdClass, bindings, prefix);
					} catch(ConsoleOptionException coe) {
						// bad option name.
					}
					return null;
				}
			});
			}

	}


}