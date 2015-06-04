package uk.ac.gla.cvr.gluetools.core.datafield.populator;

import uk.ac.gla.cvr.gluetools.core.datafield.populator.xml.XPathDataFieldPopulatorRule;
import uk.ac.gla.cvr.gluetools.core.datafield.populator.xml.XPathNodeSelectorRule;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.utils.Multiton;

public class DataFieldPopulatorRuleFactory extends PluginFactory<DataFieldPopulatorRule>{

	public static Multiton.Creator<DataFieldPopulatorRuleFactory> creator = new
			Multiton.SuppliedCreator<>(DataFieldPopulatorRuleFactory.class, DataFieldPopulatorRuleFactory::new);
	
	private DataFieldPopulatorRuleFactory() {
		super();
		registerPluginClass(XPathDataFieldPopulatorRule.ELEM_NAME, XPathDataFieldPopulatorRule.class);
		registerPluginClass(XPathNodeSelectorRule.ELEM_NAME, XPathNodeSelectorRule.class);
	}
	
}
