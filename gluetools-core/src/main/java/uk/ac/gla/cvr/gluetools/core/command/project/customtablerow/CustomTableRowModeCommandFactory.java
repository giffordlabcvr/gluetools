package uk.ac.gla.cvr.gluetools.core.command.project.customtablerow;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectMode;
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

		ConfigurableObjectMode.registerConfigurableObjectCommands(this);

		registerCommandClass(RenderObjectCommand.class);

		registerCommandClass(ExitCommand.class);
	}
	

}
