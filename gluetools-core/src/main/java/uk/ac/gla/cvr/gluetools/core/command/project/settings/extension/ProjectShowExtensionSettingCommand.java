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

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSetting;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSettingOption;


@CommandClass( 
		commandWords = {"show", "extension-setting"},
		docoptUsages = {"<extensionName> <extSettingName>"}, 
		metaTags = { },
		description = "Show the current value of a extension setting")
public class ProjectShowExtensionSettingCommand extends ProjectExtensionSettingCommand<MapResult> {

	@Override
	public MapResult execute(CommandContext cmdContext) {
		ProjectSettingOption extSettingOption = getExtensionSettingOption();
		String extensionName = getExtensionName();
		String nameText = extSettingOption.getName();
		String extSettingKey = getExtSettingKey();
		boolean isDefault;
		ProjectSetting extensionSetting = 
				GlueDataObject.lookup(cmdContext, ProjectSetting.class, 
						ProjectSetting.pkMap(extSettingKey), true);
		String valueText;
		if(extensionSetting == null) {
			valueText = extSettingOption.getDefaultValue();
			isDefault = true;
		} else {
			valueText = extensionSetting.getValue();
			isDefault = false;
		}
		Map<String, Object> resultMap = new LinkedHashMap<String, Object>();
		resultMap.put("extensionName", extensionName);
		resultMap.put("extSetttingName", nameText);
		resultMap.put("extSettingValue", valueText);
		resultMap.put("settingDescription", extSettingOption.getDescription());
		resultMap.put("isDefault", new Boolean(isDefault));
		return new MapResult("projectShowExtensionSettingResult", resultMap);
	}

	@CompleterClass
	public static class Completer extends ExtSettingCompleter {}

}