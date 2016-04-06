package uk.ac.gla.cvr.gluetools.core.command.project.settings;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSetting;


@CommandClass( 
		commandWords = {"unset", "setting"},
		docoptUsages = {"<settingName>"}, 
		metaTags = { CmdMeta.updatesDatabase },
		description = "Unset a project setting's value",
		furtherHelp = "After unsetting, the default value will be in effect")
public class ProjectUnsetSettingCommand extends ProjectSettingCommand<DeleteResult> {
	
	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		getProjectSettingOption().onSet(cmdContext, null);
		DeleteResult deleteResult = 
				GlueDataObject.delete(cmdContext, ProjectSetting.class, 
						ProjectSetting.pkMap(getProjectSettingOption().name()), true);
		cmdContext.commit();
		return deleteResult;
	}

	@CompleterClass
	public static class Completer extends SettingNameCompleter {}

}