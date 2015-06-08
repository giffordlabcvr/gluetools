package uk.ac.gla.cvr.gluetools.core.datafield.populator.xml;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.datafield.populator.NodeSelectorRule;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException.Code;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class XPathNodesRule extends NodeSelectorRule implements Plugin {

	public static String ELEM_NAME = "xPathNodes";
	
	@Override
	public void configureLocal(Element configElem) {
		configureXPathExpression(configElem);
	}
	
	protected void configureXPathExpression(Element configElem) {
		String xPathExpressionExpression = "xPathExpression/text()";
		String xPathExpressionString = PluginUtils.configureString(configElem, xPathExpressionExpression, true);
		XPath xPathEngine = XmlUtils.createXPathEngine();
		try {
			setXPathExpression(xPathEngine.compile(xPathExpressionString));
		} catch (XPathExpressionException xpee) {
			throw new PluginConfigException(xpee, Code.CONFIG_FORMAT_ERROR, 
					xPathExpressionExpression, xpee.getLocalizedMessage(), xPathExpressionString);
		}
	}

	
}
