package uk.ac.gla.cvr.gluetools.core.reporting.alignmentColumnSelector;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

@PluginClass(elemName="nucleotideRegionSelector")
public class NucleotideRegionSelector extends RegionSelector {

	private String startNt;
	private String endNt;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.startNt = PluginUtils.configureStringProperty(configElem, "startNt", true);
		this.endNt = PluginUtils.configureStringProperty(configElem, "endNt", true);
	}

}
