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