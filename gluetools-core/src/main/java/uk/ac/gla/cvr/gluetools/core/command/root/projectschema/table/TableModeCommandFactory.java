package uk.ac.gla.cvr.gluetools.core.command.root.projectschema.table;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandGroup;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class TableModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<TableModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(TableModeCommandFactory.class, TableModeCommandFactory::new);

	private TableModeCommandFactory() {
	}	

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();

		setCmdGroup(new CommandGroup("customFields", "Commands for managing custom fields", 25, false));
		registerCommandClass(CreateFieldCommand.class);
		registerCommandClass(DeleteFieldCommand.class);
		registerCommandClass(ListFieldCommand.class);
		
		setCmdGroup(CommandGroup.MODE_NAVIGATION);
		registerCommandClass(ExitCommand.class);
	}
	

}
