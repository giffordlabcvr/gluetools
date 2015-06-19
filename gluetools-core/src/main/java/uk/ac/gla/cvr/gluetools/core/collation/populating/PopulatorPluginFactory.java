package uk.ac.gla.cvr.gluetools.core.collation.populating;

import uk.ac.gla.cvr.gluetools.core.datafield.populator.DataFieldPopulator;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class PopulatorPluginFactory extends PluginFactory<PopulatorPlugin>{

	public static Multiton.Creator<PopulatorPluginFactory> creator = new
			Multiton.SuppliedCreator<>(PopulatorPluginFactory.class, PopulatorPluginFactory::new);
	
	private PopulatorPluginFactory() {
		super();
		registerPluginClass(DataFieldPopulator.class);
	}
	
}
