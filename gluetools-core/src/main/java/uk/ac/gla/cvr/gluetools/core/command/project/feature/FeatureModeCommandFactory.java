package uk.ac.gla.cvr.gluetools.core.command.project.feature;

import uk.ac.gla.cvr.gluetools.core.command.BaseCommandFactory;
import uk.ac.gla.cvr.gluetools.core.command.CommandGroup;
import uk.ac.gla.cvr.gluetools.core.command.configurableobject.ConfigurableObjectMode;
import uk.ac.gla.cvr.gluetools.core.command.console.ExitCommand;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class FeatureModeCommandFactory extends BaseCommandFactory {

	public static Multiton.Creator<FeatureModeCommandFactory> creator = new
			Multiton.SuppliedCreator<>(FeatureModeCommandFactory.class, FeatureModeCommandFactory::new);

	private FeatureModeCommandFactory() {
	}	

	@Override
	protected void populateCommandTree() {
		super.populateCommandTree();

		setCmdGroup(new CommandGroup("parent", "Commands for managing feature parent-child relationships", 50, false));
		registerCommandClass(FeatureSetParentCommand.class);
		registerCommandClass(FeatureUnsetParentCommand.class);
		registerCommandClass(FeatureShowParentCommand.class);

		ConfigurableObjectMode.registerConfigurableObjectCommands(this);
		
		setCmdGroup(new CommandGroup("metatags", "Commands for managing feature metatags", 51, false));
		registerCommandClass(FeatureSetMetatagCommand.class);
		registerCommandClass(FeatureUnsetMetatagCommand.class);
		registerCommandClass(FeatureListMetatagCommand.class);

		setCmdGroup(CommandGroup.VALIDATION);
		registerCommandClass(FeatureValidateCommand.class);
		
		setCmdGroup(new CommandGroup("segments", "Commands for querying feature location segments", 52, false));
		registerCommandClass(FeatureShowLocationSegmentsCommand.class);
		
		setCmdGroup(CommandGroup.MODE_NAVIGATION);
		registerCommandClass(ExitCommand.class);
	}
	

}
