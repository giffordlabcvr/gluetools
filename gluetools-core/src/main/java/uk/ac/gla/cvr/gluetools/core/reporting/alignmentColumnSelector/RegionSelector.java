package uk.ac.gla.cvr.gluetools.core.reporting.alignmentColumnSelector;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public class RegionSelector implements Plugin {

	private String referenceName;
	private String featureName;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		this.referenceName = PluginUtils.configureStringProperty(configElem, "referenceName", true);
		this.featureName = PluginUtils.configureStringProperty(configElem, "featureName", true);
	}
	
}
