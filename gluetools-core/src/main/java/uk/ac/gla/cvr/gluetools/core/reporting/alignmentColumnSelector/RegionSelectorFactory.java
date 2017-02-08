package uk.ac.gla.cvr.gluetools.core.reporting.alignmentColumnSelector;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class RegionSelectorFactory extends PluginFactory<RegionSelector> {

	public static Multiton.Creator<RegionSelectorFactory> creator = new
			Multiton.SuppliedCreator<>(RegionSelectorFactory.class, RegionSelectorFactory::new);
	
	private RegionSelectorFactory() {
		super();
		registerPluginClass(NucleotideRegionSelector.class);
		registerPluginClass(AminoAcidRegionSelector.class);
	}
	
}
