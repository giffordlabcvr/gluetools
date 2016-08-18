package uk.ac.gla.cvr.gluetools.core.command.project.customtablerow;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.core.command.render.RenderObjectCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class CustomTableRowModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<CustomTableRowModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(CustomTableRowModeCommandFactory.class, CustomTableRowModeCommandFactory::new);

	private CustomTableRowModeCommandFactory() {
	}	

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();

		registerCommandClass(CustomTableRowSetFieldCommand.class);
		registerCommandClass(CustomTableRowUnsetFieldCommand.class);
		registerCommandClass(CustomTableRowShowPropertyCommand.class);
		registerCommandClass(CustomTableRowListPropertyCommand.class);

		registerCommandClass(CustomTableRowSetLinkCommand.class);
		
		registerCommandClass(RenderObjectCommand.class);

		registerCommandClass(ExitCommand.class);
	}
	

}
