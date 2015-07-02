package uk.ac.gla.cvr.gluetools.core.collation.populating.xml;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;

public class XmlPopulatorRuleFactory extends PluginFactory<XmlPopulatorRule>{

	protected XmlPopulatorRuleFactory() {
		super();
		registerPluginClass(XmlFieldPopulatorRule.class);
		registerPluginClass(XPathNodesRule.class);
	}
	
	@Override
	protected XmlPopulatorRule instantiatePlugin(Element element,
			Class<? extends XmlPopulatorRule> pluginClass) {
		XmlPopulatorRule xmlPopulatorRule = super.instantiatePlugin(element, pluginClass);
		xmlPopulatorRule.setRuleFactory(this);
		return xmlPopulatorRule;
	}

}
