package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandGroup;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectMode;
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
		
		setCmdGroup(new CommandGroup("segments", "Commands for managing feature segments", 50, false));
		registerCommandClass(AddFeatureSegmentCommand.class);
		registerCommandClass(RemoveFeatureSegmentCommand.class);
		registerCommandClass(ListFeatureSegmentCommand.class);
		
		setCmdGroup(new CommandGroup("variations", "Commands for managing variations", 51, false));
		registerCommandClass(CreateVariationCommand.class);
		registerCommandClass(DeleteVariationCommand.class);
		registerCommandClass(FeatureLocListVariationCommand.class);
		
		setCmdGroup(new CommandGroup("aminos", "Commands for querying amino-acids and labeled codons", 52, false));
		registerCommandClass(FeatureLocListLabeledCodonsCommand.class);
		registerCommandClass(FeatureLocAminoAcidCommand.class);
		registerCommandClass(FeatureLocCountAminoAcidCommand.class);

		setCmdGroup(CommandGroup.VALIDATION);
		registerCommandClass(FeatureLocValidateCommand.class);

		ConfigurableObjectMode.registerConfigurableObjectCommands(this);

		setCmdGroup(CommandGroup.MODE_NAVIGATION);
		registerCommandClass(VariationCommand.class);
		registerCommandClass(ExitCommand.class);

		setCmdGroup(null);
		registerCommandClass(FeatureLocGenerateGlueConfigCommand.class);
}
	

}
