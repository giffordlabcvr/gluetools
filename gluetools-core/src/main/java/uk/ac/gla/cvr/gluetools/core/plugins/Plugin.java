package uk.ac.gla.cvr.gluetools.core.plugins;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.modules.PropertyGroup;

public interface Plugin {

	
	public default void configure(PluginConfigContext pluginConfigContext, Element configElem) {
	}

	public default void configurePropertyGroup(PropertyGroup propertyGroup) {
	}

}
