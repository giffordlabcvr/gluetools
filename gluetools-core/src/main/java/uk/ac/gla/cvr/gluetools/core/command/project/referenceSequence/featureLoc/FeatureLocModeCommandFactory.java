package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class FeatureLocModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<FeatureLocModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(FeatureLocModeCommandFactory.class, FeatureLocModeCommandFactory::new);

	private FeatureLocModeCommandFactory() {
	}	

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();
		registerCommandClass(AddFeatureSegmentCommand.class);
		registerCommandClass(RemoveFeatureSegmentCommand.class);
		registerCommandClass(ListFeatureSegmentCommand.class);
		
		registerCommandClass(VariationCommand.class);
		registerCommandClass(CreateVariationCommand.class);
		registerCommandClass(DeleteVariationCommand.class);
		registerCommandClass(FeatureLocListVariationCommand.class);
		
		registerCommandClass(FeatureLocGenerateGlueConfigCommand.class);
		registerCommandClass(FeatureLocValidateCommand.class);
		registerCommandClass(FeatureLocListLabeledCodonsCommand.class);
		
		registerCommandClass(ExitCommand.class);
	}
	

}
