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

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.command.result.OkResult;
import uk.ac.gla.cvr.gluetools.core.command.result.UpdateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSetting;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSettingException;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSettingOption;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@CommandClass( 
		commandWords = {"set", "setting"},
		docoptUsages = {"<settingName> <settingValue>"}, 
		metaTags = { CmdMeta.updatesDatabase },
		description = "Set a value for a project setting")
public class ProjectSetSettingCommand extends ProjectSettingCommand<OkResult> {
	
	public static final String SETTING_VALUE = "settingValue";
	
	private String settingValue;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		settingValue = PluginUtils.configureStringProperty(configElem, SETTING_VALUE, true);
		ProjectSettingOption projectSettingOption = getProjectSettingOption();
		String[] allowedValues = projectSettingOption.getAllowedValues();
		if(allowedValues != null) {
			List<String> valuesList = Arrays.asList(allowedValues);
			if(!valuesList.contains(settingValue)) {
				throw new ProjectSettingException(ProjectSettingException.Code.INVALID_SETTING_VALUE, projectSettingOption.getName(), settingValue, valuesList.toString());
			}
		}
	}
	
	@Override
	public OkResult execute(CommandContext cmdContext) {
		ProjectSettingOption projectSettingOption = getProjectSettingOption();
		ProjectSetting existingSetting = GlueDataObject.lookup(cmdContext, ProjectSetting.class, ProjectSetting.pkMap(projectSettingOption.name()), true);
		projectSettingOption.onSet(cmdContext, existingSetting == null ? null: existingSetting.getValue(), settingValue);
		if(existingSetting == null) {
			ProjectSetting newSetting = GlueDataObject.create(cmdContext, ProjectSetting.class, ProjectSetting.pkMap(projectSettingOption.name()), false);
			newSetting.setValue(settingValue);
			cmdContext.commit();
			return new CreateResult(ProjectSetting.class, 1);
		} else {
			existingSetting.setValue(settingValue);
			cmdContext.commit();
			return new UpdateResult(ProjectSetting.class, 1);
		}
	}
	
	
	
	@CompleterClass
	public static class Completer extends SettingNameCompleter {

		public Completer() {
			super();
			registerVariableInstantiator(SETTING_VALUE, new VariableInstantiator() {
				@Override
				@SuppressWarnings("rawtypes")
				protected List<CompletionSuggestion> instantiate(
						ConsoleCommandContext cmdContext, Class<? extends Command> cmdClass,
						Map<String, Object> bindings, String prefix) {
					try {
						String settingName = (String) bindings.get("settingName");
						ProjectSettingOption projectSettingOption = lookupSettingOptionByName(settingName);
						String[] allowedValues = projectSettingOption.getAllowedValues();
						if(allowedValues != null) {
							return Arrays.asList(allowedValues).stream().map(s -> new CompletionSuggestion(s, true)).collect(Collectors.toList());
						}
					} catch(ProjectSettingException coe) {
						// bad setting name.
					}
					return null;
				}
			});
			}

	}


}