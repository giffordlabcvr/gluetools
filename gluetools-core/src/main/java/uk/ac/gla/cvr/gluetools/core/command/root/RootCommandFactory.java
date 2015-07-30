package uk.ac.gla.cvr.gluetools.core.command.root;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class RootCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<RootCommandFactory> creator = new
			Multiton.SuppliedCreator<>(RootCommandFactory.class, RootCommandFactory::new);

	private RootCommandFactory() {
	}	

	protected void populateCommandTree() {
		super.populateCommandTree();
		registerCommandClass(ProjectCommand.class);
		registerCommandClass(CreateProjectCommand.class);
		registerCommandClass(DeleteProjectCommand.class);
		registerCommandClass(ListProjectCommand.class);
		registerCommandClass(ProjectSchemaCommand.class);
	}
}
