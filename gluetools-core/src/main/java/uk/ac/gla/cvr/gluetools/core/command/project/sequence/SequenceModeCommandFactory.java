package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.core.command.render.RenderObjectCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class SequenceModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<SequenceModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(SequenceModeCommandFactory.class, SequenceModeCommandFactory::new);

	private SequenceModeCommandFactory() {
	}	

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();
		ConfigurableObjectMode.registerConfigurableObjectCommands(this);

		registerCommandClass(ShowOriginalDataCommand.class);
		registerCommandClass(SequenceShowLengthCommand.class);
		registerCommandClass(ShowNucleotidesCommand.class);

		registerCommandClass(RenderObjectCommand.class);
		registerCommandClass(ExitCommand.class);
	}
	

}
