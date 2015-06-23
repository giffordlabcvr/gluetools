package uk.ac.gla.cvr.gluetools.core.command.project;

import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.project.field.CreateFieldCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.field.DeleteFieldCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.field.ListFieldsCommand;
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
public class ProjectModeCommandFactory extends CommandFactory {

	public static Multiton.Creator<ProjectModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(ProjectModeCommandFactory.class, ProjectModeCommandFactory::new);

	public ProjectModeCommandFactory() {
		super();
		registerPluginClass(CreateSourceCommand.class);
		registerPluginClass(DeleteSourceCommand.class);
		registerPluginClass(ListSourcesCommand.class);

		registerPluginClass(CreateModuleCommand.class);
		registerPluginClass(DeleteModuleCommand.class);
		registerPluginClass(ListModulesCommand.class);
		registerPluginClass(ShowModuleCommand.class);
		registerPluginClass(RunModuleCommand.class);

		registerPluginClass(CreateFieldCommand.class);
		registerPluginClass(DeleteFieldCommand.class);
		registerPluginClass(ListFieldsCommand.class);

		registerPluginClass(CreateSequenceCommand.class);
		registerPluginClass(DeleteSequenceCommand.class);
		registerPluginClass(ListSequencesCommand.class);
		registerPluginClass(ShowSequenceCommand.class);
		registerPluginClass(SetSequenceFieldCommand.class);

	}
	

}
