package uk.ac.gla.cvr.gluetools.core.datafield.populator;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class DataFieldPopulatorFactory extends PluginFactory<DataFieldPopulator>{

	public static Multiton.Creator<DataFieldPopulatorFactory> creator = new
			Multiton.SuppliedCreator<>(DataFieldPopulatorFactory.class, DataFieldPopulatorFactory::new);
	
	private DataFieldPopulatorFactory() {
		super();
		registerPluginClass(DataFieldPopulator.ELEM_NAME, DataFieldPopulator.class);
	}
	
}
