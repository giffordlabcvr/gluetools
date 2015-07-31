package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class SequenceModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<SequenceModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(SequenceModeCommandFactory.class, SequenceModeCommandFactory::new);

	private SequenceModeCommandFactory() {
	}	

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();
		registerCommandClass(SetFieldCommand.class);
		registerCommandClass(UnsetFieldCommand.class);
		registerCommandClass(ShowFieldCommand.class);
		registerCommandClass(ShowOriginalDataCommand.class);
		registerCommandClass(ShowLength.class);
		registerCommandClass(ShowNucleotidesCommand.class);
		registerCommandClass(ExitCommand.class);
	}
	

}
