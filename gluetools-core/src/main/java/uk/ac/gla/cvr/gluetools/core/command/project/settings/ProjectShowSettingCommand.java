package uk.ac.gla.cvr.gluetools.core.command.project.settings;

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
		commandWords = {"show", "setting"},
		docoptUsages = {"<settingName>"}, 
		metaTags = { },
		description = "Show the current value of a project setting")
public class ProjectShowSettingCommand extends ProjectSettingCommand<MapResult> {

	@Override
	public MapResult execute(CommandContext cmdContext) {
		ProjectSettingOption projectSettingOption = getProjectSettingOption();
		String nameText = projectSettingOption.getName();
		boolean isDefault;
		ProjectSetting projectSetting = 
				GlueDataObject.lookup(cmdContext.getObjectContext(), ProjectSetting.class, 
						ProjectSetting.pkMap(projectSettingOption.name()), true);
		String valueText;
		if(projectSetting == null) {
			valueText = projectSettingOption.getDefaultValue();
			isDefault = true;
		} else {
			valueText = projectSetting.getValue();
			isDefault = false;
		}
		Map<String, Object> resultMap = new LinkedHashMap<String, Object>();
		resultMap.put("settingName", nameText);
		resultMap.put("settingValue", valueText);
		resultMap.put("settingDescription", projectSettingOption.getDescription());
		resultMap.put("isDefault", new Boolean(isDefault));
		return new MapResult("projectShowSettingResult", resultMap);
	}

	@CompleterClass
	public static class Completer extends SettingNameCompleter {}

}