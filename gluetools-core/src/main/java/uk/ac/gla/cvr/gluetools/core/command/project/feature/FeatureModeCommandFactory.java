package uk.ac.gla.cvr.gluetools.core.command.project.feature;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class FeatureModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<FeatureModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(FeatureModeCommandFactory.class, FeatureModeCommandFactory::new);

	private FeatureModeCommandFactory() {
	}	

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();

		registerCommandClass(ExitCommand.class);
	}
	

}