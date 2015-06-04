package uk.ac.gla.cvr.gluetools.core.plugins;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException.Code;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class PluginUtils {

	public static String configureString(Element configElem, String xPathExpression, String defaultValue)  {
		String configured = configureString(configElem, xPathExpression, false);
		if(configured == null) {
			return defaultValue;
		}
		return configured;
	}
	
	public static String configureString(Element configElem, String xPathExpression, boolean required)  {
		String configured = XmlUtils.getXPathString(configElem, xPathExpression);
		if(required && configured == null) {
			throw new PluginConfigException(Code.REQUIRED_CONFIG_MISSING, configElem.getNodeName(), xPathExpression);
		}
		return configured;
	}

	public static Integer configureInt(Element configElem, String xPathExpression, int defaultValue)  {
		Integer configured = configureInt(configElem, xPathExpression, false);
		if(configured == null) {
			return defaultValue;
		}
		return configured;
	}
	
	public static Integer configureInt(Element configElem, String xPathExpression, boolean required)  {
		String configuredString = XmlUtils.getXPathString(configElem, xPathExpression);
		if(required && configuredString == null) {
			throw new PluginConfigException(Code.REQUIRED_CONFIG_MISSING, configElem.getNodeName(), xPathExpression);
		}
		if(configuredString == null) { return null; }
		try {
			return Integer.parseInt(configuredString);
		} catch(NumberFormatException nfe) {
			throw new PluginConfigException(Code.CONFIG_FORMAT_ERROR, configElem.getNodeName(), xPathExpression, nfe.getLocalizedMessage());
		}
	}

	public static Element findConfigElement(Element configElem, String xPathExpression)  {
		return findConfigElement(configElem, xPathExpression, false);
	}

	public static Element findConfigElement(Element configElem, String xPathExpression, boolean required)  {
		Element configSubElem = XmlUtils.getXPathElement(configElem, xPathExpression);
		if(configSubElem != null) {
			return (Element) configSubElem;
		} else if(required) {
			throw new PluginConfigException(Code.REQUIRED_CONFIG_MISSING, configElem.getNodeName(), xPathExpression);
		} else {
			return null;
		}
	}

	public static List<Element> findConfigElements(Element configElem, String xPathExpression)  {
		return findConfigElements(configElem, xPathExpression, null, null);
	}

	public static List<Element> findConfigElements(Element configElem, String xPathExpression, Integer min, Integer max)  {
		List<Element> configSubElems = XmlUtils.getXPathElements(configElem, xPathExpression);
		int size = configSubElems.size();
		if(max != null && size > max) {
			throw new PluginConfigException(Code.TOO_MANY_CONFIG_ELEMENTS, configElem.getNodeName(), xPathExpression, size, max);
		}
		if(min != null && size < min) {
			throw new PluginConfigException(Code.TOO_FEW_CONFIG_ELEMENTS, configElem.getNodeName(), xPathExpression, size, max);
		}
		return configSubElems;
	}

}
