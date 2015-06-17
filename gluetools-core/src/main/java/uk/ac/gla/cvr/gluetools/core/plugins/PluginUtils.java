package uk.ac.gla.cvr.gluetools.core.plugins;

import java.util.List;
import java.util.stream.Collectors;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
		Node configuredNode = XmlUtils.getXPathNode(configElem, xPathExpression);
		if(required && configuredNode == null) {
			throw new PluginConfigException(Code.REQUIRED_CONFIG_MISSING, xPathExpression);
		}
		if(configuredNode == null) {
			return null;
		}
		setValidConfig(configElem, configuredNode);
		return XmlUtils.getNodeText(configuredNode);
	}

	public static Integer configureInt(Element configElem, String xPathExpression, int defaultValue)  {
		Integer configured = configureInt(configElem, xPathExpression, false);
		if(configured == null) {
			return defaultValue;
		}
		return configured;
	}
	
	public static Integer configureInt(Element configElem, String xPathExpression, boolean required)  {
		String configuredString = configureString(configElem, xPathExpression, required);
		if(configuredString == null) { return null; }
		try {
			return Integer.parseInt(configuredString);
		} catch(NumberFormatException nfe) {
			throw new PluginConfigException(nfe, Code.CONFIG_FORMAT_ERROR, xPathExpression, "Not an integer", configuredString);
		}
	}

	public static Element findConfigElement(Element configElem, String xPathExpression)  {
		return findConfigElement(configElem, xPathExpression, false);
	}

	public static Element findConfigElement(Element configElem, String xPathExpression, boolean required)  {
		Element configSubElem = XmlUtils.getXPathElement(configElem, xPathExpression);
		if(configSubElem != null) {
			setValidConfig(configElem, configSubElem);
			return (Element) configSubElem;
		} else if(required) {
			throw new PluginConfigException(Code.REQUIRED_CONFIG_MISSING, xPathExpression);
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
		configSubElems.forEach(elem -> setValidConfig(configElem, elem));
		return configSubElems;
	}
	
	/**
	 * Sets the valid config flag to true for all the Element / Attribute ancestors of a node, 
	 * up to, but not including the configElem.
	 * @param configElem
	 * @param node
	 */
	public static void setValidConfig(Element configElem, Node node) {
		while(!(node instanceof Attr || node instanceof Element)) {
			node = node.getParentNode();
		}
		while(node != configElem) {
			setValidConfigLocal(node);
			if(node instanceof Attr) {
				node = ((Attr) node).getOwnerElement();
			} else {
				node = node.getParentNode();
			}
		}
	}

	/**
	 * Sets the valid config flag to true for a node, 
	 */
	public static void setValidConfigLocal(Node node) {
		node.setUserData("glueTools.PluginUtils", Boolean.TRUE, null);
	}

	public static boolean isValidConfig(Node node) {
		Object userData = node.getUserData("glueTools.PluginUtils");
		return(userData != null && userData instanceof Boolean && ((Boolean) userData));
	}

	public static <E extends Enum<E>> E configureEnum(Class<E> enumClass, Element configElem, String xPathExpression, E defaultValue) {
		E configuredValue = configureEnum(enumClass, configElem, xPathExpression, false);
		if(configuredValue == null) {
			return defaultValue;
		}
		return configuredValue;
	}
	
	public static <E extends Enum<E>> E configureEnum(Class<E> enumClass, Element configElem, String xPathExpression, boolean required) {
		String configuredString = configureString(configElem, xPathExpression, required);
		if(configuredString == null) { return null; }
		try {
			return Enum.valueOf(enumClass, configuredString);
		} catch(IllegalArgumentException iae) {
			throw new PluginConfigException(iae, Code.CONFIG_FORMAT_ERROR, xPathExpression, iae.getLocalizedMessage());
		}
	}

	public static List<String> configureStrings(Element configElem, String xPathExpression, boolean required) {
		List<Node> configuredNodes = XmlUtils.getXPathNodes(configElem, xPathExpression);
		if(required && configuredNodes.isEmpty()) {
			throw new PluginConfigException(Code.REQUIRED_CONFIG_MISSING, xPathExpression);
		}
		configuredNodes.forEach(c -> setValidConfig(configElem, c));
		return configuredNodes.stream().map(c -> XmlUtils.getNodeText(c)).collect(Collectors.toList());
	}

	public static void checkValidConfig(Element element) {
		checkValidConfig(element, "");
	}

	private static void checkValidConfig(Element element, String xPathBase) {
		NodeList childNodes = element.getChildNodes();
		for(int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if(node instanceof Element) {
				String xPath = xPathBase+node.getNodeName();
				if(!PluginUtils.isValidConfig(node)) {
					throw new PluginConfigException(PluginConfigException.Code.UNKNOWN_CONFIG_ELEMENT, xPath);
				}
				checkValidConfig((Element) node, xPath+"/");
			} 
		}
		NamedNodeMap attributes = element.getAttributes();
		for(int j = 0; j < attributes.getLength(); j++) {
			Node node = attributes.item(j);
			if(node instanceof Attr) {
				if(!PluginUtils.isValidConfig(node)) {
					throw new PluginConfigException(PluginConfigException.Code.UNKNOWN_CONFIG_ATTRIBUTE, xPathBase+"@"+node.getNodeName());
				}
			}
		}
	}

	
}
