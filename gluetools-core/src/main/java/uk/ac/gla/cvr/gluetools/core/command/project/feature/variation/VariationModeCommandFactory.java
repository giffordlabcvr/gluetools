package uk.ac.gla.cvr.gluetools.core.command.project.feature.variation;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class VariationModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<VariationModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(VariationModeCommandFactory.class, VariationModeCommandFactory::new);

	private VariationModeCommandFactory() {
	}	

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();
		
		registerCommandClass(ExitCommand.class);
	}
	

}
