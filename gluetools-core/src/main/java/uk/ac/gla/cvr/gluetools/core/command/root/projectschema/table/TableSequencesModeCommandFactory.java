package uk.ac.gla.cvr.gluetools.core.command.root.projectschema.table;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class TableSequencesModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<TableSequencesModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(TableSequencesModeCommandFactory.class, TableSequencesModeCommandFactory::new);

	private TableSequencesModeCommandFactory() {
	}	

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();
		registerCommandClass(CreateFieldCommand.class);
		registerCommandClass(DeleteFieldCommand.class);
		registerCommandClass(ListFieldCommand.class);
		registerCommandClass(ExitCommand.class);
	}
	

}
