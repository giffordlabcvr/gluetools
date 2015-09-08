package uk.ac.gla.cvr.gluetools.core.collation.populating.xml;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@PluginClass(elemName="xPathNodes")
public class XPathNodesRule extends NodeSelectorRule implements Plugin {

	
	@Override
	public void configureLocal(PluginConfigContext pluginConfigContext, Element configElem) {
		configureXPathExpression(configElem);
	}
	
	protected void configureXPathExpression(Element configElem) {
		String xPathExpressionExpression = "xPathExpression/text()";
		String xPathExpressionString = PluginUtils.configureString(configElem, xPathExpressionExpression, true);
		XPath xPathEngine = GlueXmlUtils.createXPathEngine();
		try {
			setXPathExpression(xPathEngine.compile(xPathExpressionString));
		} catch (XPathExpressionException xpee) {
			throw new PluginConfigException(xpee, Code.CONFIG_FORMAT_ERROR, 
					xPathExpressionExpression, xpee.getLocalizedMessage(), xPathExpressionString);
		}
	}

}
