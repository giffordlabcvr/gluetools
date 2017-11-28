package uk.ac.gla.cvr.gluetools.core.command.root.projectschema;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandGroup;
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
		setCmdGroup(CommandGroup.MODE_NAVIGATION);
		registerCommandClass(TableCommand.class);
		registerCommandClass(ExitCommand.class);

		setCmdGroup(new CommandGroup("customTables", "Commands for managing custom tables", 25, false));
		registerCommandClass(CreateCustomTableCommand.class);
		registerCommandClass(DeleteCustomTableCommand.class);
		registerCommandClass(ListCustomTableCommand.class);

		setCmdGroup(new CommandGroup("customLinks", "Commands for managing custom relational links", 26, false));
		registerCommandClass(CreateLinkCommand.class);
		registerCommandClass(DeleteLinkCommand.class);
		registerCommandClass(ListLinkCommand.class);

	}
	

}
