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

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();
		
		registerCommandClass(CreateSourceCommand.class);
		registerCommandClass(DeleteSourceCommand.class);
		registerCommandClass(ListSourceCommand.class);

		registerCommandClass(CreateModuleCommand.class);
		registerCommandClass(DeleteModuleCommand.class);
		registerCommandClass(ListModuleCommand.class);

		registerCommandClass(CreateSequenceCommand.class);
		registerCommandClass(DeleteSequenceCommand.class);
		registerCommandClass(ListSequenceCommand.class);

		registerCommandClass(ModuleCommand.class);
		registerCommandClass(SequenceCommand.class);

		
		addGroupHelp(Arrays.asList("create"), "Create a new object in this project");
		addGroupHelp(Arrays.asList("list"), "List certain objects in this project");
		addGroupHelp(Arrays.asList("delete"), "Delete a certain object from this project");

		
		registerCommandClass(ExitCommand.class);
	}
	

}
