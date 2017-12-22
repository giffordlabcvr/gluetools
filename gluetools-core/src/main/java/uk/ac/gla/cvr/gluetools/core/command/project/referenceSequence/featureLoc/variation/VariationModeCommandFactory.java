package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.variation;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandGroup;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.core.command.render.RenderObjectCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class VariationModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<VariationModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(VariationModeCommandFactory.class, VariationModeCommandFactory::new);

	private VariationModeCommandFactory() {
	}	

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();
		
		setCmdGroup(new CommandGroup("pattern-locs", "Commands for managing pattern locations", 50, false));
		registerCommandClass(VariationCreatePatternLocCommand.class);
		registerCommandClass(VariationDeletePatternLocCommand.class);
		registerCommandClass(VariationListPatternLocCommand.class);
		registerCommandClass(VariationListPatternLocCodonCommand.class);

		ConfigurableObjectMode.registerConfigurableObjectCommands(this);

		setCmdGroup(CommandGroup.VALIDATION);
		registerCommandClass(VariationValidateCommand.class);

		setCmdGroup(new CommandGroup("var-almt-notes", "Commands for managing variation-alignment notes", 51, false));
		registerCommandClass(VariationCreateAlmtNoteCommand.class);
		registerCommandClass(VariationDeleteAlmtNoteCommand.class);
		registerCommandClass(VariationListAlmtNoteCommand.class);
		
		setCmdGroup(CommandGroup.RENDERING);
		registerCommandClass(RenderObjectCommand.class);

		setCmdGroup(CommandGroup.MODE_NAVIGATION);
		registerCommandClass(VariationAlmtNoteCommand.class);
		registerCommandClass(ExitCommand.class);

		setCmdGroup(null);
		registerCommandClass(VariationGenerateGlueConfigCommand.class);

	}
	

}
