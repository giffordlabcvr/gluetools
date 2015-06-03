package uk.ac.gla.cvr.gluetools.core.plugins;

import org.w3c.dom.Element;

public interface Plugin {

	
	public void configure(Element configElem) throws PluginConfigException;
	
}
