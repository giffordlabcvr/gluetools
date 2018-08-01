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
package uk.ac.gla.cvr.gluetools.core.command.project;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandGroup;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.core.command.console.ReturnToProjectModeCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.settings.ProjectSetSettingCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.settings.ProjectShowSettingCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.settings.ProjectUnsetSettingCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.settings.extension.ProjectSetExtensionSettingCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.settings.extension.ProjectShowExtensionSettingCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.settings.extension.ProjectUnsetExtensionSettingCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

// TODO -- mode command factories parameterized by mode command base class?
// TODO -- generic list / delete / show commands?
public class ProjectModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<ProjectModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(ProjectModeCommandFactory.class, ProjectModeCommandFactory::new);

	private ProjectModeCommandFactory() {
	}	

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();
		
		setCmdGroup(CommandGroup.MODE_NAVIGATION);
		registerCommandClass(ModuleCommand.class);
		registerCommandClass(SequenceCommand.class);
		registerCommandClass(ReferenceSequenceCommand.class);
		registerCommandClass(FeatureCommand.class);
		registerCommandClass(AlignmentCommand.class);
		registerCommandClass(ExitCommand.class);
		registerCommandClass(ReturnToProjectModeCommand.class);


		setCmdGroup(new CommandGroup("sequences", "Commands for managing sequence objects", 5, false));
		registerCommandClass(CreateSourceCommand.class);
		registerCommandClass(DeleteSourceCommand.class);
		registerCommandClass(ListSourceCommand.class);
		registerCommandClass(ImportSourceCommand.class);
		registerCommandClass(ExportSourceCommand.class);
		registerCommandClass(CreateSequenceCommand.class);
		registerCommandClass(ImportSequenceCommand.class);
		registerCommandClass(DeleteSequenceCommand.class);
		registerCommandClass(ListSequenceCommand.class);
		registerCommandClass(WebListSequenceCommand.class);
		registerCommandClass(MoveSequenceCommand.class);
		registerCommandClass(CountSequenceCommand.class);
		registerCommandClass(CopySequenceCommand.class);
		registerCommandClass(ConcatenateSequenceCommand.class);
		registerCommandClass(ListFormatSequenceCommand.class);
		registerCommandClass(ExportSequenceCommand.class);

		
		setCmdGroup(new CommandGroup("alignments", "Commands for managing alignment objects", 6, false));
		registerCommandClass(CreateAlignmentCommand.class);
		registerCommandClass(DeleteAlignmentCommand.class);
		registerCommandClass(ListAlignmentCommand.class);
		registerCommandClass(ComputeAlignmentCommand.class);
		registerCommandClass(ExtendAlignmentCommand.class);
		registerCommandClass(TranslateSegmentsCommand.class);
		registerCommandClass(ImportAlignmentCommand.class);



		setCmdGroup(new CommandGroup("references", "Commands for managing reference sequence objects", 7, false));
		registerCommandClass(CreateReferenceSequenceCommand.class);
		registerCommandClass(DeleteReferenceSequenceCommand.class);
		registerCommandClass(ListReferenceSequenceCommand.class);

		setCmdGroup(new CommandGroup("features", "Commands for managing feature objects", 8, false));
		registerCommandClass(CreateFeatureCommand.class);
		registerCommandClass(DeleteFeatureCommand.class);
		registerCommandClass(ListFeatureCommand.class);

		setCmdGroup(new CommandGroup("custom-table-row", "Commands for objects in custom tables", 9, false));
		registerCommandClass(CreateCustomTableRowCommand.class);
		registerCommandClass(DeleteCustomTableRowCommand.class);
		registerCommandClass(CustomTableRowCommand.class);
		registerCommandClass(ListCustomTableRowCommand.class);
		registerCommandClass(CountCustomTableRowCommand.class);

		setCmdGroup(new CommandGroup("project-settings", "Commands for managing project / extension settings", 10, false));
		registerCommandClass(ProjectSetSettingCommand.class);
		registerCommandClass(ProjectUnsetSettingCommand.class);
		registerCommandClass(ProjectShowSettingCommand.class);
		registerCommandClass(ProjectSetExtensionSettingCommand.class);
		registerCommandClass(ProjectUnsetExtensionSettingCommand.class);
		registerCommandClass(ProjectShowExtensionSettingCommand.class);
		
		setCmdGroup(new CommandGroup("modules", "Commands for managing modules", 11, false));
		registerCommandClass(ImportModuleCommand.class);
		registerCommandClass(CreateModuleCommand.class);
		registerCommandClass(DeleteModuleCommand.class);
		registerCommandClass(ListModuleCommand.class);

		
		setCmdGroup(new CommandGroup("bulk-db", "Commands for bulk database operations", 12, false));
		registerCommandClass(MultiSetFieldCommand.class);
		registerCommandClass(MultiUnsetFieldCommand.class);
		registerCommandClass(MultiCopyFieldCommand.class);
		registerCommandClass(MultiDeleteCommand.class);
		registerCommandClass(MultiRenderCommand.class);
		registerCommandClass(MultiUnsetLinkTargetCommand.class);
		registerCommandClass(ListVariationCommand.class);
		registerCommandClass(CountVariationCommand.class);
		registerCommandClass(ListAlmtMemberCommand.class);
		registerCommandClass(CountAlmtMemberCommand.class);
		registerCommandClass(ListVarAlmtNoteCommand.class);
		registerCommandClass(CountVarAlmtNoteCommand.class);
		registerCommandClass(ListMemberFLocNoteCommand.class);
		registerCommandClass(CountMemberFLocNoteCommand.class);

		
		setCmdGroup(CommandGroup.OTHER);
		registerCommandClass(ProjectValidateCommand.class);
		registerCommandClass(ProjectGenerateGlueConfigCommand.class);
	}
	

}
