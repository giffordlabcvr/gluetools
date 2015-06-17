package uk.ac.gla.cvr.gluetools.core.plugins;

import org.w3c.dom.Element;

public interface Plugin {

	
	public default void configure(PluginConfigContext pluginConfigContext, Element configElem) {
	}
	
}
