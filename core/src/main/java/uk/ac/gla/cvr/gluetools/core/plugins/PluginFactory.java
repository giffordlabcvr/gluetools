package uk.ac.gla.cvr.gluetools.core.plugins;

import java.util.LinkedHashMap;
import java.util.Map;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactoryException.Code;

public class PluginFactory<P extends Plugin> {

	private String configRootElemName;
	private String thisFactoryName;
	
	private Map<String, Class<? extends P>> typeStringToPluginClass = 
			new LinkedHashMap<String, Class<? extends P>>();
	
	protected void registerPluginClass(String type, Class<? extends P> theClass) {
		typeStringToPluginClass.put(type, theClass);
	}
	
	protected PluginFactory(String configRootElemName) {
		super();
		this.configRootElemName = configRootElemName;
		this.thisFactoryName = this.getClass().getSimpleName();
	}

	public P createFromXml(Element element) throws PluginFactoryException {
		if(!element.getNodeName().equals(configRootElemName)) {
			throw new PluginFactoryException(Code.INCORRECT_ROOT_ELEMENT, thisFactoryName, configRootElemName);
		}
		String type = element.getAttribute("type");
		if(type == null) {
			throw new PluginFactoryException(Code.MISSING_TYPE_ATTRIBUTE, thisFactoryName, configRootElemName);
		}
		Class<? extends P> pluginClass = typeStringToPluginClass.get(type);
		if(pluginClass == null) {
			throw new PluginFactoryException(Code.UNKNOWN_PLUGIN_TYPE, thisFactoryName, type);
		}
		P plugin = null;
		try {
			plugin = pluginClass.newInstance();
		} catch(Exception e) {
			throw new PluginFactoryException(e, Code.PLUGIN_CREATION_FAILED, thisFactoryName, type);
		}
		try {
			plugin.configure(element);
		} catch(Exception e) {
			throw new PluginFactoryException(e, Code.PLUGIN_CONFIGURATION_FAILED, thisFactoryName, type);
		}
		return plugin;
	}
	
}
