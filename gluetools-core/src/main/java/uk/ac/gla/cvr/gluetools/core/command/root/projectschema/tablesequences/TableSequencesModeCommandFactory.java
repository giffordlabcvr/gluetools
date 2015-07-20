package uk.ac.gla.cvr.gluetools.core.command.root.projectschema.tablesequences;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class TableSequencesModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<TableSequencesModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(TableSequencesModeCommandFactory.class, TableSequencesModeCommandFactory::new);

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();
		registerCommandClass(CreateSequenceFieldCommand.class);
		registerCommandClass(DeleteSequenceFieldCommand.class);
		registerCommandClass(ListSequenceFieldCommand.class);
		registerCommandClass(ExitCommand.class);
	}
	

}
