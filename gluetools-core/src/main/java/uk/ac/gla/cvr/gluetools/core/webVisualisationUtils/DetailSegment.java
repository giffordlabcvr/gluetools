package uk.ac.gla.cvr.gluetools.core.webVisualisationUtils;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

public class DetailSegment extends ReferenceSegment {

	private String id;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.id = PluginUtils.configureStringProperty(configElem, "id", false);
	}

	public String getId() {
		return id;
	}
	
}
