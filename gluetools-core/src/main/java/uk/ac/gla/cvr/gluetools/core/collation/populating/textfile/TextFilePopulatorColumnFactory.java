package uk.ac.gla.cvr.gluetools.core.collation.populating.textfile;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class TextFilePopulatorColumnFactory extends PluginFactory<BaseTextFilePopulatorColumn> {

	public static Multiton.Creator<TextFilePopulatorColumnFactory> creator = new
			Multiton.SuppliedCreator<>(TextFilePopulatorColumnFactory.class, TextFilePopulatorColumnFactory::new);
	
	private TextFilePopulatorColumnFactory() {
		super();
		registerPluginClass(TextFilePopulatorColumn.class);
		registerPluginClass(IsoCountryTextFilePopulatorColumn.class);
	}

	
	
}
