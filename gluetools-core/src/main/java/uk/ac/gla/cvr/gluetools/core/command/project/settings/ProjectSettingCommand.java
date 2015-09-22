package uk.ac.gla.cvr.gluetools.core.command.project.settings;

import java.util.LinkedList;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandCompleter;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.ProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSettingException;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSettingOption;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public abstract class ProjectSettingCommand<R extends CommandResult> extends ProjectModeCommand<R> {

	private ProjectSettingOption projectSettingOption;

	protected ProjectSettingOption getProjectSettingOption() {
		return projectSettingOption;
	}
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		String settingName = PluginUtils.configureStringProperty(configElem, "settingName", true);
		projectSettingOption = lookupSettingOptionByName(settingName);
	}

	
	protected static ProjectSettingOption lookupSettingOptionByName(String optionName) {
		for(ProjectSettingOption option : ProjectSettingOption.values()) {
			if(option.getName().equals(optionName)) {
				return option;
			}
		}
		throw new ProjectSettingException(ProjectSettingException.Code.NO_SUCH_SETTING, optionName);
	}
	
	@SuppressWarnings("rawtypes")
	public abstract static class SettingNameCompleter extends CommandCompleter {
		@Override
		public List<String> completionSuggestions(ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass, List<String> argStrings) {
			if(argStrings.isEmpty()) {
				LinkedList<String> suggestions = new LinkedList<String>();
				for(ProjectSettingOption projectSettingOption : ProjectSettingOption.values()) {
					suggestions.add(projectSettingOption.getName());
				}
				return suggestions;
			}
			return super.completionSuggestions(cmdContext, cmdClass, argStrings);
		}
	}
	
	
}
