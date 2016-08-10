package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.Arrays;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.settings.ProjectSetSettingCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.settings.ProjectShowSettingCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.settings.ProjectUnsetSettingCommand;
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
		
		registerCommandClass(CreateAlignmentCommand.class);
		registerCommandClass(DeleteAlignmentCommand.class);
		registerCommandClass(ListAlignmentCommand.class);

		registerCommandClass(ComputeAlignmentCommand.class);
		registerCommandClass(ExtendAlignmentCommand.class);
		
		registerCommandClass(TranslateSegmentsCommand.class);
		registerCommandClass(CreateSourceCommand.class);
		registerCommandClass(DeleteSourceCommand.class);
		registerCommandClass(ListSourceCommand.class);
		registerCommandClass(ImportSourceCommand.class);
		registerCommandClass(ExportSourceCommand.class);

		registerCommandClass(ImportModuleCommand.class);
		registerCommandClass(CreateModuleCommand.class);
		registerCommandClass(DeleteModuleCommand.class);
		registerCommandClass(ListModuleCommand.class);

		registerCommandClass(CreateSequenceCommand.class);
		registerCommandClass(ImportSequenceCommand.class);
		registerCommandClass(DeleteSequenceCommand.class);
		registerCommandClass(ListSequenceCommand.class);
		registerCommandClass(MoveSequenceCommand.class);
		registerCommandClass(CopySequenceCommand.class);
		registerCommandClass(MultiSetFieldCommand.class);
		registerCommandClass(MultiUnsetFieldCommand.class);
		registerCommandClass(MultiCopyFieldCommand.class);
		registerCommandClass(MultiDeleteCommand.class);
		registerCommandClass(ConcatenateSequenceCommand.class);

		registerCommandClass(CreateReferenceSequenceCommand.class);
		registerCommandClass(DeleteReferenceSequenceCommand.class);
		registerCommandClass(ListReferenceSequenceCommand.class);

		registerCommandClass(CreateFeatureCommand.class);
		registerCommandClass(DeleteFeatureCommand.class);
		registerCommandClass(ListFeatureCommand.class);

		registerCommandClass(ListVariationCommand.class);
		registerCommandClass(ListVarAlmtNoteCommand.class);
		
		registerCommandClass(ProjectSetSettingCommand.class);
		registerCommandClass(ProjectUnsetSettingCommand.class);
		registerCommandClass(ProjectShowSettingCommand.class);
		
		registerCommandClass(ModuleCommand.class);
		registerCommandClass(SequenceCommand.class);
		registerCommandClass(ReferenceSequenceCommand.class);
		registerCommandClass(FeatureCommand.class);
		registerCommandClass(AlignmentCommand.class);
		
		registerCommandClass(ListFormatSequenceCommand.class);
		registerCommandClass(ExportSequenceCommand.class);

		registerCommandClass(ProjectValidateCommand.class);

		registerCommandClass(ProjectGenerateGlueConfigCommand.class);

		registerCommandClass(CreateCustomTableRowCommand.class);
		registerCommandClass(DeleteCustomTableRowCommand.class);
		
		// remove this when no longer needed
		registerCommandClass(CorrectAlignmentCommand.class);
		
		addGroupHelp(Arrays.asList("create"), "Create a new object in this project");
		addGroupHelp(Arrays.asList("list"), "List certain objects in this project");
		addGroupHelp(Arrays.asList("delete"), "Delete a certain object from this project");


		
		registerCommandClass(ExitCommand.class);
	}
	

}
