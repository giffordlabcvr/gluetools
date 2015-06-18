package uk.ac.gla.cvr.gluetools.core.command.project;

import uk.ac.gla.cvr.gluetools.core.command.CommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.project.field.CreateFieldCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.field.DeleteFieldCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.field.ListFieldsCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.importer.CreateImporterCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.importer.DeleteImporterCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.importer.ListImportersCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.importer.ShowImporterCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.populator.CreatePopulatorCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.populator.DeletePopulatorCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.populator.ListPopulatorsCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.CreateSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.DeleteSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.ListSequencesCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.ShowSequenceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.source.CreateSourceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.source.DeleteSourceCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.source.ListSourcesCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

// TODO -- mode command factories parameterized by mode command base class?

public class ProjectModeCommandFactory extends CommandFactory {

	public static Multiton.Creator<ProjectModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(ProjectModeCommandFactory.class, ProjectModeCommandFactory::new);

	public ProjectModeCommandFactory() {
		super();
		registerPluginClass(CreateSourceCommand.class);
		registerPluginClass(DeleteSourceCommand.class);
		registerPluginClass(ListSourcesCommand.class);

		registerPluginClass(CreatePopulatorCommand.class);
		registerPluginClass(DeletePopulatorCommand.class);
		registerPluginClass(ListPopulatorsCommand.class);

		registerPluginClass(CreateImporterCommand.class);
		registerPluginClass(DeleteImporterCommand.class);
		registerPluginClass(ListImportersCommand.class);
		registerPluginClass(ShowImporterCommand.class);

		registerPluginClass(CreateFieldCommand.class);
		registerPluginClass(DeleteFieldCommand.class);
		registerPluginClass(ListFieldsCommand.class);

		registerPluginClass(CreateSequenceCommand.class);
		registerPluginClass(DeleteSequenceCommand.class);
		registerPluginClass(ListSequencesCommand.class);
		registerPluginClass(ShowSequenceCommand.class);

	}
	

}
