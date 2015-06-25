package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.Arrays;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.CreateModuleCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.DeleteModuleCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ListModulesCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.RunModuleCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.module.ShowModuleCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.CreateSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.DeleteSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.ListSequencesCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.SetSequenceFieldCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.ShowSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.source.CreateSourceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.source.DeleteSourceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.source.ListSourcesCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

// TODO -- mode command factories parameterized by mode command base class?
// TODO -- generic list / delete / show commands?
public class ProjectModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<ProjectModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(ProjectModeCommandFactory.class, ProjectModeCommandFactory::new);

	public ProjectModeCommandFactory() {
		super();
		
		registerCommandClass(CreateSourceCommand.class);
		registerCommandClass(DeleteSourceCommand.class);
		registerCommandClass(ListSourcesCommand.class);

		registerCommandClass(CreateModuleCommand.class);
		registerCommandClass(DeleteModuleCommand.class);
		registerCommandClass(ListModulesCommand.class);
		registerCommandClass(ShowModuleCommand.class);
		registerCommandClass(RunModuleCommand.class);

		registerCommandClass(CreateSequenceCommand.class);
		registerCommandClass(DeleteSequenceCommand.class);
		registerCommandClass(ListSequencesCommand.class);
		registerCommandClass(ShowSequenceCommand.class);
		registerCommandClass(SetSequenceFieldCommand.class);

		addGroupHelp(Arrays.asList("create"), "Create a new object in this project");
		addGroupHelp(Arrays.asList("list"), "List certain objects in this project");
		addGroupHelp(Arrays.asList("delete"), "Delete a certain object from this project");
		addGroupHelp(Arrays.asList("show"), "Describe in detail a certain objecthelp");

		
		registerCommandClass(ExitCommand.class);
	}
	

}
