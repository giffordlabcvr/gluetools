package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.feature;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class FeatureModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<FeatureModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(FeatureModeCommandFactory.class, FeatureModeCommandFactory::new);

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();
		registerCommandClass(AddFeatureSegmentCommand.class);
		registerCommandClass(RemoveFeatureSegmentCommand.class);
		registerCommandClass(ListFeatureSegmentCommand.class);
		
		registerCommandClass(ExitCommand.class);
	}
	

}