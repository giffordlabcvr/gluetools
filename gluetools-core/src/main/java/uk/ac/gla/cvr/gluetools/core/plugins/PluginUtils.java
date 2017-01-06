package uk.ac.gla.cvr.gluetools.core.plugins;

import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionException;
import org.biojava.nbio.core.exceptions.CompoundNotFoundException;
import org.biojava.nbio.core.sequence.DNASequence;
import org.biojava.nbio.core.sequence.compound.AmbiguityDNACompoundSet;
import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException.Code;
import uk.ac.gla.cvr.gluetools.utils.CayenneUtils;
import uk.ac.gla.cvr.gluetools.utils.CommandDocumentXmlUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.Template;

// TODO stop using XPaths when it's just a simple property lookup.
public class PluginUtils {

	
	public static byte[] configureBase64BytesProperty(Element configElem, String propertyName, boolean required) {
		String base64String = PluginUtils.configureStringProperty(configElem, propertyName, required);
		if(base64String == null) {
			return null;
		}
		try {
			return Base64.getDecoder().decode(base64String);
		} catch(IllegalArgumentException pe) {
			throw new PluginConfigException(Code.PROPERTY_FORMAT_ERROR, propertyName, pe.getLocalizedMessage(), base64String);
		}
	}
	
	public static Template configureFreemarkerTemplateProperty(PluginConfigContext pluginConfigContext, 
			Element configElem, String propertyName, boolean required) {
		String templateString = PluginUtils.configureStringProperty(configElem, propertyName, required);
		Template template = null;
		if(templateString != null) {
			Configuration freemarkerConfiguration = pluginConfigContext.getFreemarkerConfiguration();
			try {
				template = templateFromString(templateString, freemarkerConfiguration);
			} catch(ParseException pe) {
				throw new PluginConfigException(Code.PROPERTY_FORMAT_ERROR, propertyName, pe.getLocalizedMessage(), templateString);
			} 
		}
		return template;
	}

	public static Template templateFromString(String templateString, Configuration freemarkerConfiguration) throws ParseException {
		try {
			Template template = new Template(UUID.randomUUID().toString(), new StringReader(templateString), freemarkerConfiguration);
			return template;
		} catch (IOException e) {
			if(e instanceof ParseException) {
				throw (ParseException) e;
			}
			throw new RuntimeException(e);
		} 
	}
	
	public static Pattern configureRegexPatternProperty(Element configElem, String propertyName, boolean required) {
		String patternString = PluginUtils.configureStringProperty(configElem, propertyName, required);
		if(patternString != null) {
			return parseRegexPattern(propertyName, patternString);
		} else {
			return null;
		}
	}

	public static Pattern parseRegexPattern(String propertyName,
			String patternString) {
		try {
			return Pattern.compile(patternString);
		} catch(PatternSyntaxException pse) {
			throw new PluginConfigException(Code.PROPERTY_FORMAT_ERROR, propertyName, pse.getLocalizedMessage(), patternString);
		}
	}

	
	public static DNASequence parseNucleotidesProperty(Element configElem, String propertyName, boolean required) {
		String ntsString = PluginUtils.configureStringProperty(configElem, propertyName, required);
		DNASequence dnaSequence = null;
		if(ntsString != null) {
			try {
				dnaSequence = new DNASequence(ntsString, AmbiguityDNACompoundSet.getDNACompoundSet());
			} catch (CompoundNotFoundException cnfe) {
				throw new PluginConfigException(Code.PROPERTY_FORMAT_ERROR, 
						propertyName, cnfe.getLocalizedMessage(), ntsString);
			}
		}
		return dnaSequence;
	}

	
	
	public static Expression configureCayenneExpressionProperty(Element configElem, String propertyName, boolean required) {
		String expressionString = PluginUtils.configureStringProperty(configElem, propertyName, required);
		Expression expression = null;
		if(expressionString != null) {
			expressionString = expressionString.replaceAll("\\t\\r\\n", "");
			try {
				expression = CayenneUtils.expressionFromString(expressionString);
			} catch(ExpressionException ee) {
				throw new PluginConfigException(ee, PluginConfigException.Code.PROPERTY_FORMAT_ERROR, propertyName, 
						ee.getLocalizedMessage(), expressionString);
			} catch(Exception e) {
				throw new PluginConfigException(e, PluginConfigException.Code.PROPERTY_FORMAT_ERROR, propertyName, 
						e.getLocalizedMessage(), expressionString);
			}
		}
		return expression;
	}
	
	public static String configureStringProperty(Element configElem, String propertyName, String defaultValue) {
		String configured = configureStringProperty(configElem, propertyName, false);
		if(configured != null) {
			return configured;
		}
		return defaultValue;
	}

	public static String configureStringProperty(Element configElem, String propertyName, List<String> allowedValues, boolean required) {
		String result = configureStringProperty(configElem, propertyName, required);
		if(result != null && !allowedValues.contains(result)) {
			throw new PluginConfigException(PluginConfigException.Code.PROPERTY_FORMAT_ERROR, propertyName, 
					"Allowed values: "+allowedValues, result);
		}
		return result;
	}

	public static String configureStringProperty(Element configElem, String propertyName, boolean required) {
		List<Element> propertyElems = GlueXmlUtils.findChildElements(configElem, propertyName);
		if(propertyElems.isEmpty()) {
			if(required) {
				throw new PluginConfigException(Code.REQUIRED_PROPERTY_MISSING, configElem.getNodeName(), propertyName);
			} else {
				return null;
			}
		}
		propertyElems.forEach(e -> setValidConfig(configElem, e));
		if(propertyElems.size() > 1) {
			throw new PluginConfigException(Code.MULTIPLE_PROPERTY_SETTINGS, propertyName);
		}
		return propertyElems.get(0).getTextContent();
	}

	public static char configureCharProperty(Element configElem, String propertyName, boolean required) {
		String string = configureStringProperty(configElem, propertyName, required);
		if(string.length() != 1) {
			throw new PluginConfigException(PluginConfigException.Code.PROPERTY_FORMAT_ERROR, propertyName, 
					"Single character required", string);
		}
		return string.charAt(0);
	}

	
	
	public static List<String> configureStringsProperty(Element configElem, String propertyName) {
		return configureStringsProperty(configElem, propertyName, null, null);
	}
	
	public static List<String> configureStringsProperty(Element configElem, String propertyName, 
			Integer min, Integer max) {
		List<Element> propertyElems = GlueXmlUtils.findChildElements(configElem, propertyName);
		int size = propertyElems.size();
		if(max != null && size > max) {
			throw new PluginConfigException(Code.TOO_FEW_PROPERTY_VALUES, propertyName, size, max);
		}
		if(min != null && size < min) {
			throw new PluginConfigException(Code.TOO_MANY_PROPERTY_VALUES, propertyName, size, max);
		}
		propertyElems.forEach(e -> setValidConfig(configElem, e));
		return propertyElems.stream().map(Element::getTextContent).collect(Collectors.toList());
	}
	
	public static String configureString(Element configElem, String xPathExpression, String defaultValue)  {
		String configured = configureString(configElem, xPathExpression, false);
		if(configured == null) {
			return defaultValue;
		}
		return configured;
	}
	
	public static String configureString(Element configElem, String xPathExpression, boolean required)  {
		Node configuredNode = GlueXmlUtils.getXPathNode(configElem, xPathExpression);
		if(required && configuredNode == null) {
			throw new PluginConfigException(Code.REQUIRED_CONFIG_MISSING, xPathExpression);
		}
		if(configuredNode == null) {
			return null;
		}
		setValidConfig(configElem, configuredNode);
		return GlueXmlUtils.getNodeText(configuredNode);
	}

	public static Integer configureIntProperty(Element configElem, String propertyName, int defaultValue)  {
		Integer configured = configureIntProperty(configElem, propertyName, false);
		if(configured == null) {
			return defaultValue;
		}
		return configured;
	}
	
	
	public static Integer configureIntProperty(Element configElem, String propertyName, 
			Integer minValue, boolean minInclusive, 
			Integer maxValue, boolean maxInclusive,
			boolean required)  {
		String configuredString = configureStringProperty(configElem, propertyName, required);
		if(configuredString == null) { return null; }
		Integer result = null;
		try {
			result = Integer.parseInt(configuredString);
		} catch(NumberFormatException nfe) {
			throw new PluginConfigException(Code.PROPERTY_FORMAT_ERROR, propertyName, "Not an integer", configuredString);
		}
		if(result != null) {
			if(minValue != null) {
				if(minInclusive) {
					if(result < minValue) {
						throw new PluginConfigException(Code.PROPERTY_VALUE_OUT_OF_RANGE, propertyName, result, "<", minValue);
					}
				} else if(result <= minValue) {
					throw new PluginConfigException(Code.PROPERTY_VALUE_OUT_OF_RANGE, propertyName, result, "<=", minValue);
				}
			}
			if(maxValue != null) {
				if(maxInclusive) {
					if(result > maxValue) {
						throw new PluginConfigException(Code.PROPERTY_VALUE_OUT_OF_RANGE, propertyName, result, ">", maxValue);
					}
				} else if(result <= maxValue) {
					throw new PluginConfigException(Code.PROPERTY_VALUE_OUT_OF_RANGE, propertyName, result, ">=", maxValue);
				}
			}
		}
		
		return result;
	}

	
	public static Integer configureIntProperty(Element configElem, String propertyName, boolean required)  {
		return configureIntProperty(configElem, propertyName, null, false, null, false, required);
	}
	
	
	public static Double configureDoubleProperty(Element configElem, String propertyName, double defaultValue)  {
		Double configured = configureDoubleProperty(configElem, propertyName, false);
		if(configured == null) {
			return defaultValue;
		}
		return configured;
	}

	public static Double configureDoubleProperty(Element configElem, String propertyName, boolean required)  {
		return configureDoubleProperty(configElem, propertyName, null, false, null, false, required);
	}
	
	public static Double configureDoubleProperty(Element configElem, String propertyName, 
			Double minValue, boolean minInclusive, 
			Double maxValue, boolean maxInclusive,
			boolean required)  {
		String configuredString = configureStringProperty(configElem, propertyName, required);
		if(configuredString == null) { return null; }
		Double result = null;
		try {
			result = Double.parseDouble(configuredString);
		} catch(NumberFormatException nfe) {
			throw new PluginConfigException(Code.PROPERTY_FORMAT_ERROR, propertyName, "Not a double", configuredString);
		}
		if(result != null) {
			if(minValue != null) {
				if(minInclusive) {
					if(result < minValue) {
						throw new PluginConfigException(Code.PROPERTY_VALUE_OUT_OF_RANGE, propertyName, result, "<", minValue);
					}
				} else if(result <= minValue) {
					throw new PluginConfigException(Code.PROPERTY_VALUE_OUT_OF_RANGE, propertyName, result, "<=", minValue);
				}
			}
			if(maxValue != null) {
				if(maxInclusive) {
					if(result > maxValue) {
						throw new PluginConfigException(Code.PROPERTY_VALUE_OUT_OF_RANGE, propertyName, result, ">", maxValue);
					}
				} else if(result >= maxValue) {
					throw new PluginConfigException(Code.PROPERTY_VALUE_OUT_OF_RANGE, propertyName, result, ">=", maxValue);
				}
			}
		}
		
		return result;
	}
	


	public static Boolean configureBooleanProperty(Element configElem, String propertyName, boolean required)  {
		String configuredString = configureStringProperty(configElem, propertyName, required);
		if(configuredString == null) { return null; }
		return Boolean.parseBoolean(configuredString);
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
			throw new PluginConfigException(Code.CONFIG_FORMAT_ERROR, xPathExpression, "Not an integer", configuredString);
		}
	}

	public static Element findConfigElement(Element configElem, String xPathExpression)  {
		return findConfigElement(configElem, xPathExpression, false);
	}

	public static Element findConfigElement(Element configElem, String xPathExpression, boolean required)  {
		Element configSubElem = GlueXmlUtils.getXPathElement(configElem, xPathExpression);
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
		List<Element> configSubElems = GlueXmlUtils.getXPathElements(configElem, xPathExpression);
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

	
	public static <E extends Enum<E>> E configureEnumProperty(Class<E> enumClass, Element configElem, String propertyName, E defaultValue) {
		E configuredValue = configureEnumProperty(enumClass, configElem, propertyName, false);
		if(configuredValue == null) {
			return defaultValue;
		}
		return configuredValue;
	}
	
	public static <E extends Enum<E>> E configureEnumProperty(Class<E> enumClass, Element configElem, String propertyName, boolean required) {
		String configuredString = configureStringProperty(configElem, propertyName, required);
		if(configuredString == null) { return null; }
		try {
			return Enum.valueOf(enumClass, configuredString);
		} catch(IllegalArgumentException iae) {
			String msg = "Allowed values: "+Arrays.asList(enumClass.getEnumConstants());
			throw new PluginConfigException(Code.PROPERTY_FORMAT_ERROR, propertyName, msg, configuredString);
		}
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
			String msg = "Allowed values: "+Arrays.asList(enumClass.getEnumConstants());
			throw new PluginConfigException(Code.CONFIG_FORMAT_ERROR, xPathExpression, msg, configuredString);
		}
	}

	public static List<String> configureStrings(Element configElem, String xPathExpression, boolean required) {
		List<Node> configuredNodes = GlueXmlUtils.getXPathNodes(configElem, xPathExpression);
		if(required && configuredNodes.isEmpty()) {
			throw new PluginConfigException(Code.REQUIRED_CONFIG_MISSING, xPathExpression);
		}
		configuredNodes.forEach(c -> setValidConfig(configElem, c));
		return configuredNodes.stream().map(c -> GlueXmlUtils.getNodeText(c)).collect(Collectors.toList());
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
				String nodeName = node.getNodeName();
				if(!nodeName.equals(CommandDocumentXmlUtils.GLUE_TYPE_ATTRIBUTE)) {
					if(!PluginUtils.isValidConfig(node)) {
						throw new PluginConfigException(PluginConfigException.Code.UNKNOWN_CONFIG_ATTRIBUTE, xPathBase+"@"+nodeName);
					}
				}
			}
		}
	}


	public static String configureIdentifierProperty(Element configElem, String propertyName, boolean required) {
		String identifierValue = configureStringProperty(configElem, propertyName, required);
		if(identifierValue != null && !identifierValue.matches("[a-z][a-z0-9_]*")) {
			throw new PluginConfigException(Code.PROPERTY_FORMAT_ERROR, propertyName, 
					"Identifier must begin with a lower-case letter, and may contain lower-case letters, digits and underscores.", propertyName);
		}
		return identifierValue;
	}

}
