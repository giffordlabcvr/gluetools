package uk.ac.gla.cvr.gluetools.core.requestGatekeeper;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.requestQueue.Request;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

/**
 * Configured with an XPath that should return a boolean.
 * This XPath is run on the XML document representation of the command, e.g.:
 * 
  <invoke-function>
   <functionName glueType="String">myFunction</functionName>
   <argument glueType="String[]">bananas</argument>
   <argument glueType="String[]">apples</argument>
  </invoke-function>
 * 
 * (Use "console set echo-cmd-xml true" to see the XML representation on the console).
 * 
 * The XPath could be:
   invoke-function/functionName/text() = 'myFunction' and invoke-function/argument[1]/text() = 'bananas'
 *
 * You can use an online XPath tester to test this.
 *
 */
@PluginClass(elemName="commandXPathFilter")
public class CommandXPathRequestFilter extends BaseRequestFilter {

	private XPathExpression xPathExpression;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		XPath xPath = GlueXmlUtils.createXPathEngine();;
		this.xPathExpression = GlueXmlUtils.compileXPathExpression(xPath, PluginUtils.configureStringProperty(configElem, "xPath", true));
	}

	@Override
	protected boolean allowRequestLocal(Request request) {
		Document cmdXmlDoc = request.getCommand().getCmdElem().getOwnerDocument();
		return (Boolean) GlueXmlUtils.runXPath(cmdXmlDoc, xPathExpression, XPathConstants.BOOLEAN);
	}

}
