package uk.ac.gla.cvr.gluetools.core.collation.populating.xml;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;

public class XmlPopulatorRuleFactory extends PluginFactory<XmlPopulatorRule>{

	protected XmlPopulatorRuleFactory() {
		super();
		registerPluginClass(IsoCountryPropertyPopulatorRule.class);
		registerPluginClass(XmlPropertyPopulatorRule.class);
		registerPluginClass(XPathNodesRule.class);

		// deprecated
		registerPluginClass(IsoCountryFieldPopulatorRule.class);
		registerPluginClass(XmlFieldPopulatorRule.class);
	}
	
	@Override
	public XmlPopulatorRule instantiateFromElement(Element element) {
		XmlPopulatorRule xmlPopulatorRule = super.instantiateFromElement(element);
		xmlPopulatorRule.setRuleFactory(this);
		return xmlPopulatorRule;
	}


}
