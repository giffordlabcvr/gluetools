package uk.ac.gla.cvr.gluetools.core.command.root.projectschema;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class ProjectSchemaModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<ProjectSchemaModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(ProjectSchemaModeCommandFactory.class, ProjectSchemaModeCommandFactory::new);

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();
		registerCommandClass(TableSequencesCommand.class);
		registerCommandClass(ExitCommand.class);
	}
	

}
