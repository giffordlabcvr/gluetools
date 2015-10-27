package uk.ac.gla.cvr.gluetools.core.command.project.variationCategory;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class VariationCategoryModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<VariationCategoryModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(VariationCategoryModeCommandFactory.class, VariationCategoryModeCommandFactory::new);

	private VariationCategoryModeCommandFactory() {
	}	

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();

		registerCommandClass(VariationCategorySetParentCommand.class);
		registerCommandClass(VariationCategoryUnsetParentCommand.class);
		registerCommandClass(VariationCategoryShowParentCommand.class);

		registerCommandClass(VariationCategorySetNotifiabilityCommand.class);
		registerCommandClass(VariationCategoryShowNotifiabilityCommand.class);

		registerCommandClass(ExitCommand.class);
	}
	

}
