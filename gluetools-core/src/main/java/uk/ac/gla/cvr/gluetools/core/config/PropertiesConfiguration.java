package uk.ac.gla.cvr.gluetools.core.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public class PropertiesConfiguration implements Plugin {

	private Map<String, PropertyConfig> properties = new LinkedHashMap<String, PropertyConfig>();

	public class PropertyConfig implements Plugin {
		private String name;
		private String value;
		@Override
		public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
			Plugin.super.configure(pluginConfigContext, configElem);
			name = PluginUtils.configureStringProperty(configElem, "name", true);
			value = PluginUtils.configureStringProperty(configElem, "value", true);
		}
		public String getName() {
			return name;
		}
		public String getValue() {
			return value;
		}
	} 
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		Plugin.super.configure(pluginConfigContext, configElem);
		List<Element> propertyElems = PluginUtils.findConfigElements(configElem, "property");
		propertyElems.forEach(elem -> {
			PropertyConfig propertyConfig = new PropertyConfig();
			propertyConfig.configure(pluginConfigContext, elem);
			properties.put(propertyConfig.getName(), propertyConfig);
		});
	}

	public List<String> getAllPropertyNames() {
		return new ArrayList<String>(properties.keySet());
	}
	
	public String getPropertyValue(String propertyName) {
		PropertyConfig propertyConfig = properties.get(propertyName);
		if(propertyConfig != null) {
			return propertyConfig.getValue();
		} else {
			return null;
		}
	}
	
	public String getPropertyValue(String propertyName, String defaultValue) {
		String configuredValue = getPropertyValue(propertyName);
		if(configuredValue != null) {
			return configuredValue;
		}
		return defaultValue;
	}

}
