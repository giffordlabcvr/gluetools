package uk.ac.gla.cvr.gluetools.core.datafield.populator.xml;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.datafield.populator.DataFieldPopulatorRule;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public abstract class XPathPopulatorRule extends DataFieldPopulatorRule {

	private XPathExpression xPathExpression;

	@Override
	public void configure(Element configElem)  {
		super.configure(configElem);
		String xPathExpressionExpression = "xPathExpression/text()";
		String xPathExpressionString = PluginUtils.configureString(configElem, xPathExpressionExpression, true);
		XPath xPathEngine = XmlUtils.createXPathEngine();
		try {
			xPathExpression = xPathEngine.compile(xPathExpressionString);
		} catch (XPathExpressionException xpee) {
			throw new PluginConfigException(xpee, Code.CONFIG_FORMAT_ERROR, configElem.getNodeName(), 
					xPathExpressionExpression, xpee.getLocalizedMessage(), xPathExpressionString);
		}
	}
	
	protected XPathExpression getXPathExpression() {
		return xPathExpression;
	}
}
