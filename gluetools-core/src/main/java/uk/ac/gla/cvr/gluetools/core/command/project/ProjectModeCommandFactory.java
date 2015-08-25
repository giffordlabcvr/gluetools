package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.Arrays;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
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
		registerCommandClass(TranslateSegmentsCommand.class);
		registerCommandClass(CreateSourceCommand.class);
		registerCommandClass(DeleteSourceCommand.class);
		registerCommandClass(ListSourceCommand.class);

		registerCommandClass(ImportModuleCommand.class);
		registerCommandClass(DeleteModuleCommand.class);
		registerCommandClass(ListModuleCommand.class);

		registerCommandClass(CreateSequenceCommand.class);
		registerCommandClass(ImportSequenceCommand.class);
		registerCommandClass(DeleteSequenceCommand.class);
		registerCommandClass(ListSequenceCommand.class);

		registerCommandClass(CreateReferenceSequenceCommand.class);
		registerCommandClass(DeleteReferenceSequenceCommand.class);
		registerCommandClass(ListReferenceSequenceCommand.class);

		registerCommandClass(CreateFeatureCommand.class);
		registerCommandClass(DeleteFeatureCommand.class);
		registerCommandClass(ListFeatureCommand.class);

		registerCommandClass(ModuleCommand.class);
		registerCommandClass(SequenceCommand.class);
		registerCommandClass(ReferenceSequenceCommand.class);
		registerCommandClass(FeatureCommand.class);
		registerCommandClass(AlignmentCommand.class);

		
		addGroupHelp(Arrays.asList("create"), "Create a new object in this project");
		addGroupHelp(Arrays.asList("list"), "List certain objects in this project");
		addGroupHelp(Arrays.asList("delete"), "Delete a certain object from this project");

		
		registerCommandClass(ExitCommand.class);
	}
	

}
