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
package uk.ac.gla.cvr.gluetools.core.command.project.settings;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSettingException;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSettingOption;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class ProjectSettingCommand<R extends CommandResult> extends ProjectModeCommand<R> {

	public static final String SETTING_NAME = "settingName";

	private ProjectSettingOption projectSettingOption;

	protected ProjectSettingOption getProjectSettingOption() {
		return projectSettingOption;
	}
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		String settingName = PluginUtils.configureStringProperty(configElem, SETTING_NAME, true);
		projectSettingOption = lookupSettingOptionByName(settingName);
	}

	
	protected static ProjectSettingOption lookupSettingOptionByName(String optionName) {
		for(ProjectSettingOption option : ProjectSettingOption.values()) {
			if(option.getName().equals(optionName) && !option.getName().startsWith("extension-")) {
				return option;
			}
		}
		throw new ProjectSettingException(ProjectSettingException.Code.NO_SUCH_SETTING, optionName);
	}
	
	@CompleterClass
	public static class SettingNameCompleter extends AdvancedCmdCompleter {

		public SettingNameCompleter() {
			super();
			registerVariableInstantiator(SETTING_NAME, new VariableInstantiator() {
				@Override
				@SuppressWarnings("rawtypes")
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					return Arrays.asList(ProjectSettingOption.values())
							.stream()
							.filter(co -> !co.getName().startsWith("extension-"))
							.map(co -> new CompletionSuggestion(co.getName(), true))
							.collect(Collectors.toList());
				}
			});
		}
	}
	
}
