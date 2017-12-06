package uk.ac.gla.cvr.gluetools.core.command.project.settings.extension;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSetting;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSettingOption;


@CommandClass( 
		commandWords = {"unset", "extension-setting"},
		docoptUsages = {"<extensionName> <extSettingName>"}, 
		metaTags = { CmdMeta.updatesDatabase },
		description = "Unset a extension setting's value",
		furtherHelp = "After unsetting, the default value will be in effect")
public class ProjectUnsetExtensionSettingCommand extends ProjectExtensionSettingCommand<DeleteResult> {
	
	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		ProjectSettingOption extensionSettingOption = getExtensionSettingOption();
		String extSettingKey = getExtSettingKey();
		ProjectSetting existingSetting = GlueDataObject.lookup(cmdContext, ProjectSetting.class, 
				ProjectSetting.pkMap(extSettingKey), true);
		extensionSettingOption.onSet(cmdContext, existingSetting == null ? null: existingSetting.getValue(), null);
		DeleteResult deleteResult = 
				GlueDataObject.delete(cmdContext, ProjectSetting.class, 
						ProjectSetting.pkMap(extSettingKey), true);
		cmdContext.commit();
		return deleteResult;
	}

	@CompleterClass
	public static class Completer extends ExtSettingCompleter {}

}