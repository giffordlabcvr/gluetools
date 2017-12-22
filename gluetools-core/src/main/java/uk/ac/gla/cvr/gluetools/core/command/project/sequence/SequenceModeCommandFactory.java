package uk.ac.gla.cvr.gluetools.core.command.project.sequence;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandGroup;
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

		setCmdGroup(new CommandGroup("sequence", "Commands for querying sequence properties", 50, false));
		registerCommandClass(ShowOriginalDataCommand.class);
		registerCommandClass(SequenceShowLengthCommand.class);
		registerCommandClass(ShowNucleotidesCommand.class);

		setCmdGroup(CommandGroup.MODE_NAVIGATION);
		registerCommandClass(ExitCommand.class);

		setCmdGroup(CommandGroup.RENDERING);
		registerCommandClass(RenderObjectCommand.class);
	}
	

}
