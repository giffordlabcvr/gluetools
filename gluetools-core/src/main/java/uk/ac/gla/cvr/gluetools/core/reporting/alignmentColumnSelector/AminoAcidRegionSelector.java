package uk.ac.gla.cvr.gluetools.core.reporting.alignmentColumnSelector;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="aminoAcidRegionSelector")
public class AminoAcidRegionSelector extends RegionSelector {

	private String startCodon;
	private String endCodon;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.startCodon = PluginUtils.configureStringProperty(configElem, "startCodon", true);
		this.endCodon = PluginUtils.configureStringProperty(configElem, "endCodon", true);
	}
	
	
	
}
