package uk.ac.gla.cvr.gluetools.core.command.root.projectschema.tablesequences;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class TableSequencesModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<TableSequencesModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(TableSequencesModeCommandFactory.class, TableSequencesModeCommandFactory::new);

	public TableSequencesModeCommandFactory() {
		super();
		registerCommandClass(CreateSequenceFieldCommand.class);
		registerCommandClass(DeleteSequenceFieldCommand.class);
		registerCommandClass(ListSequenceFieldsCommand.class);
		registerCommandClass(ExitCommand.class);


	}
	

}
