package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class ReferenceSequenceModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<ReferenceSequenceModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(ReferenceSequenceModeCommandFactory.class, ReferenceSequenceModeCommandFactory::new);

	private ReferenceSequenceModeCommandFactory() {
	}	

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();

		registerCommandClass(AddFeatureLocCommand.class);
		registerCommandClass(RemoveFeatureLocation.class);
		registerCommandClass(ListFeatureLocCommand.class);
		registerCommandClass(FeatureLocCommand.class);
		registerCommandClass(ReferenceShowSequenceCommand.class);
		registerCommandClass(ReferenceShowCreationTimeCommand.class);
		registerCommandClass(ReferenceShowFeatureTreeCommand.class);

		registerCommandClass(ExitCommand.class);
	}
	

}
