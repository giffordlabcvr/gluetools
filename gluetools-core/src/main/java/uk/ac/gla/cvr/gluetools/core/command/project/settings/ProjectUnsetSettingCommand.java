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

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.DeleteResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSetting;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSettingOption;


@CommandClass( 
		commandWords = {"unset", "setting"},
		docoptUsages = {"<settingName>"}, 
		metaTags = {  },
		description = "Unset a project setting's value",
		furtherHelp = "After unsetting, the default value will be in effect")
public class ProjectUnsetSettingCommand extends ProjectSettingCommand<DeleteResult> {
	
	@Override
	public DeleteResult execute(CommandContext cmdContext) {
		ProjectSettingOption projectSettingOption = getProjectSettingOption();
		ProjectSetting existingSetting = GlueDataObject.lookup(cmdContext, ProjectSetting.class, ProjectSetting.pkMap(projectSettingOption.name()), true);
		projectSettingOption.onSet(cmdContext, existingSetting == null ? null: existingSetting.getValue(), null);
		DeleteResult deleteResult = 
				GlueDataObject.delete(cmdContext, ProjectSetting.class, 
						ProjectSetting.pkMap(getProjectSettingOption().name()), true);
		cmdContext.commit();
		return deleteResult;
	}

	@CompleterClass
	public static class Completer extends SettingNameCompleter {}

}