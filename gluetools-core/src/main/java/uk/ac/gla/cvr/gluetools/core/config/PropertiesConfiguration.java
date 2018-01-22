/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
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
