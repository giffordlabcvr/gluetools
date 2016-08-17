package uk.ac.gla.cvr.gluetools.core.command.root.projectschema;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class ProjectSchemaModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<ProjectSchemaModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(ProjectSchemaModeCommandFactory.class, ProjectSchemaModeCommandFactory::new);

	private ProjectSchemaModeCommandFactory() {
	}	

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();
		registerCommandClass(TableCommand.class);
		registerCommandClass(ExitCommand.class);

		registerCommandClass(CreateCustomTableCommand.class);
		registerCommandClass(DeleteCustomTableCommand.class);
		registerCommandClass(ListCustomTableCommand.class);

		registerCommandClass(CreateLinkCommand.class);
		registerCommandClass(DeleteLinkCommand.class);
		registerCommandClass(ListLinkCommand.class);

	}
	

}
