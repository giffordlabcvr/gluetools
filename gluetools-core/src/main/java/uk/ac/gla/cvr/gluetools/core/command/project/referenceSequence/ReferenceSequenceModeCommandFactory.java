package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class ReferenceSequenceModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<ReferenceSequenceModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(ReferenceSequenceModeCommandFactory.class, ReferenceSequenceModeCommandFactory::new);

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();

		registerCommandClass(CreateFeatureCommand.class);
		registerCommandClass(DeleteFeatureCommand.class);
		registerCommandClass(ListFeatureCommand.class);
		registerCommandClass(FeatureCommand.class);

		registerCommandClass(ExitCommand.class);
	}
	

}