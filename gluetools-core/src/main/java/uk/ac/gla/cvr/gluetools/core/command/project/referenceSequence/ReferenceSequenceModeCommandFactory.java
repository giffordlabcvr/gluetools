package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandGroup;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.core.command.render.RenderObjectCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class ReferenceSequenceModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<ReferenceSequenceModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(ReferenceSequenceModeCommandFactory.class, ReferenceSequenceModeCommandFactory::new);

	private ReferenceSequenceModeCommandFactory() {
	}	

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();

		setCmdGroup(new CommandGroup("feature-locs", "Commands for managing feature locations", 50, false));
		registerCommandClass(AddFeatureLocCommand.class);
		registerCommandClass(RemoveFeatureLocCommand.class);
		registerCommandClass(ListFeatureLocCommand.class);
		registerCommandClass(InheritFeatureLocationCommand.class);

		setCmdGroup(new CommandGroup("properties", "Commands for querying reference sequence properties", 52, false));
		registerCommandClass(ReferenceShowSequenceCommand.class);
		registerCommandClass(ReferenceShowCreationTimeCommand.class);
		registerCommandClass(ReferenceShowFeatureTreeCommand.class);

		setCmdGroup(CommandGroup.VALIDATION);
		registerCommandClass(ReferenceValidateCommand.class);

		setCmdGroup(new CommandGroup("variations", "Commands for managing variations", 51, false));
		registerCommandClass(ClearVariationCommand.class);

		setCmdGroup(CommandGroup.RENDERING);
		registerCommandClass(RenderObjectCommand.class);
		
		ConfigurableObjectMode.registerConfigurableObjectCommands(this);

		setCmdGroup(CommandGroup.MODE_NAVIGATION);
		registerCommandClass(FeatureLocCommand.class);
		registerCommandClass(ExitCommand.class);

		setCmdGroup(null);
		registerCommandClass(ReferenceSequenceGenerateGlueConfigCommand.class);

	}
	

}
