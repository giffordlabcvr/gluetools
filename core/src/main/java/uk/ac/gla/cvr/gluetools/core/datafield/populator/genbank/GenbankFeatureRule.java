package uk.ac.gla.cvr.gluetools.core.datafield.populator.genbank;

import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.datafield.populator.NodeSelectorRule;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class GenbankFeatureRule extends NodeSelectorRule {

	public static String ELEM_NAME = "gbFeature";

	@Override
	public void configureLocal(PluginConfigContext pluginConfigContext, Element configElem) {
		String keyXPath = "@key";
		String featureKey = PluginUtils.configureString(configElem, keyXPath, true);
		String xPathString = "/GBSeq/GBSeq_feature-table/GBFeature[GBFeature_key='"+featureKey+"']";
		try {
			setXPathExpression(XmlUtils.createXPathEngine().compile(xPathString));
		} catch (XPathExpressionException xpee) {
			throw new PluginConfigException(xpee, Code.CONFIG_FORMAT_ERROR, keyXPath, xpee.getLocalizedMessage(), featureKey);
		}
	}

}
