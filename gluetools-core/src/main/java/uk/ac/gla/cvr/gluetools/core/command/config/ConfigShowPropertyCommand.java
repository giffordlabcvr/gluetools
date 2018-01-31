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
package uk.ac.gla.cvr.gluetools.core.command.config;



import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.config.PropertiesConfiguration;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
		commandWords = {"config","show"},
		docoptUsages = {"<propertyName>"}, 
		metaTags = { CmdMeta.nonModeWrappable },
		description = "Show the value of a configuration property")
public class ConfigShowPropertyCommand extends Command<ConfigPropertyResult> {

	public static final String PROPERTY_NAME = "propertyName";
	private String propertyName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		propertyName = PluginUtils.configureStringProperty(configElem, PROPERTY_NAME, true);
	}


	@Override
	public ConfigPropertyResult execute(CommandContext cmdContext) {
		PropertiesConfiguration propertiesConfiguration = cmdContext.getGluetoolsEngine().getPropertiesConfiguration();
		String propertyValue = propertiesConfiguration.getPropertyValue(propertyName);
		return new ConfigPropertyResult(propertyName, propertyValue);
	}
	
	
	@CompleterClass
	public static class PropertyNameCompleter extends AdvancedCmdCompleter {

		public PropertyNameCompleter() {
			super();
			registerVariableInstantiator("propertyName", new VariableInstantiator() {
				@Override
				@SuppressWarnings("rawtypes")
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					return cmdContext.getGluetoolsEngine().getPropertiesConfiguration().getAllPropertyNames()
							.stream().map(pn -> new CompletionSuggestion(pn, true)).collect(Collectors.toList());
				}
			});
		}

	}


}