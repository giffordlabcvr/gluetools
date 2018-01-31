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
package uk.ac.gla.cvr.gluetools.core.command.project.settings.extension;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSetting;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSettingException;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSettingOption;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class ProjectExtensionSettingCommand<R extends CommandResult> extends ProjectModeCommand<R> {

	public static final String EXTENSION_NAME = "extensionName";
	public static final String EXT_SETTING_NAME = "extSettingName";

	private String extensionName;
	private ProjectSettingOption extensionSettingOption;

	protected String getExtensionName() {
		return extensionName;
	}
	
	protected ProjectSettingOption getExtensionSettingOption() {
		return extensionSettingOption;
	}
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.extensionName = PluginUtils.configureStringProperty(configElem, EXTENSION_NAME, true);
		String extSetttingName = PluginUtils.configureStringProperty(configElem, EXT_SETTING_NAME, true);
		this.extensionSettingOption = lookupExtSettingOptionByName(extSetttingName);
		
		if(!this.extensionName.matches("[a-zA-Z][a-zA-Z0-9_]*")) {
			throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "Invalid <extensionName> \""+this.extensionName+"\"");
		}
	}

	
	protected String getExtSettingKey() {
		return getExtensionName()+":"+getExtensionSettingOption().name();
	}

	protected static ProjectSettingOption lookupExtSettingOptionByName(String optionName) {
		for(ProjectSettingOption option : ProjectSettingOption.values()) {
			if(option.getName().equals(optionName) && option.getName().startsWith("extension-")) {
				return option;
			}
		}
		throw new ProjectSettingException(ProjectSettingException.Code.NO_SUCH_EXTENSION_SETTING, optionName);
	}
	
	@CompleterClass
	public static class ExtSettingCompleter extends AdvancedCmdCompleter {

		public ExtSettingCompleter() {
			super();
			registerVariableInstantiator(EXTENSION_NAME, new VariableInstantiator() {
				@Override
				@SuppressWarnings("rawtypes")
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					List<ProjectSetting> allSettings = GlueDataObject.query(cmdContext, ProjectSetting.class, new SelectQuery(ProjectSetting.class));
					List<CompletionSuggestion> suggestions = allSettings
							.stream()
							.filter(co -> co.getName().contains(":EXTENSION_"))
							.map(co -> {
								String extensionName = co.getName().substring(0, co.getName().indexOf(":EXTENSION_"));
								return new CompletionSuggestion(extensionName, true);
							})
							.collect(Collectors.toSet())
							.stream()
							.collect(Collectors.toList());
					if(suggestions.isEmpty()) {
						return null;
					} else {
						return suggestions;
					}
				}
			});
			registerVariableInstantiator(EXT_SETTING_NAME, new VariableInstantiator() {
				@Override
				@SuppressWarnings("rawtypes")
				public List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					ProjectSettingOption[] values = ProjectSettingOption.values();
					return Arrays.asList(values)
							.stream()
							.filter(co -> co.getName().startsWith("extension-"))
							.map(co -> new CompletionSuggestion(co.getName(), true))
							.collect(Collectors.toList());
				}
			});
		}
	}
	
}
