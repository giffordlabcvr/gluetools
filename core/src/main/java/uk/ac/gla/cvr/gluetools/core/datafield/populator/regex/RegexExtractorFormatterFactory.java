package uk.ac.gla.cvr.gluetools.core.datafield.populator.regex;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class RegexExtractorFormatterFactory extends PluginFactory<RegexExtractorFormatter>{

	public static Multiton.Creator<RegexExtractorFormatterFactory> creator = new
			Multiton.SuppliedCreator<>(RegexExtractorFormatterFactory.class, RegexExtractorFormatterFactory::new);
	
	private RegexExtractorFormatterFactory() {
		super();
		registerPluginClass(RegexExtractorFormatter.ELEM_NAME, RegexExtractorFormatter.class);
	}
	
}
