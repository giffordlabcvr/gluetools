package uk.ac.gla.cvr.gluetools.core.plugins;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException.Code;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class PluginUtils {

	public static String configureString(Element configElem, String xPath, String defaultValue) throws PluginConfigException {
		String configured = configureString(configElem, xPath, false);
		if(configured == null) {
			return defaultValue;
		}
		return configured;
	}
	
	public static String configureString(Element configElem, String xPath, boolean required) throws PluginConfigException {
		String configured = XmlUtils.getXPathString(configElem, xPath);
		if(required && configured == null) {
			throw new PluginConfigException(Code.REQUIRED_STRING_CONFIG_MISSING, configElem.getNodeName(), xPath);
		}
		return configured;
	}

	public static Integer configureInt(Element configElem, String xPath, int defaultValue) throws PluginConfigException {
		Integer configured = configureInt(configElem, xPath, false);
		if(configured == null) {
			return defaultValue;
		}
		return configured;
	}
	
	public static Integer configureInt(Element configElem, String xPath, boolean required) throws PluginConfigException {
		String configuredString = XmlUtils.getXPathString(configElem, xPath);
		if(required && configuredString == null) {
			throw new PluginConfigException(Code.REQUIRED_INTEGER_CONFIG_MISSING, configElem.getNodeName(), xPath);
		}
		if(configuredString == null) { return null; }
		try {
			return Integer.parseInt(configuredString);
		} catch(NumberFormatException nfe) {
			throw new PluginConfigException(Code.INTEGER_CONFIG_FORMAT_ERROR, configElem.getNodeName(), xPath);
		}
		
	}

	
}
