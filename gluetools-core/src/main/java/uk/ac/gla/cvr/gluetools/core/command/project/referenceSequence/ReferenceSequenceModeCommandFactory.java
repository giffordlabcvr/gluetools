package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
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

		registerCommandClass(AddFeatureLocCommand.class);
		registerCommandClass(RemoveFeatureLocCommand.class);
		registerCommandClass(ListFeatureLocCommand.class);
		registerCommandClass(FeatureLocCommand.class);
		registerCommandClass(ReferenceShowSequenceCommand.class);
		registerCommandClass(ReferenceShowCreationTimeCommand.class);
		registerCommandClass(ReferenceShowFeatureTreeCommand.class);
		registerCommandClass(ReferenceSequenceGenerateGlueConfigCommand.class);

		registerCommandClass(ReferenceValidateCommand.class);
		registerCommandClass(InheritFeatureLocationCommand.class);
		registerCommandClass(ClearVariationCommand.class);

		registerCommandClass(RenderObjectCommand.class);
		
		ConfigurableObjectMode.registerConfigurableObjectCommands(this);
		
		registerCommandClass(ExitCommand.class);
	}
	

}
