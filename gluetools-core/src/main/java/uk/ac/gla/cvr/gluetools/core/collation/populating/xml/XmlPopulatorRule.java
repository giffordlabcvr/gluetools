package uk.ac.gla.cvr.gluetools.core.collation.populating.xml;

import org.w3c.dom.Node;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;


public abstract class XmlPopulatorRule implements Plugin {

	private XmlPopulatorRuleFactory ruleFactory;
	
	protected XmlPopulatorRuleFactory getRuleFactory() {
		return ruleFactory;
	}

	void setRuleFactory(XmlPopulatorRuleFactory ruleFactory) {
		this.ruleFactory = ruleFactory;
	}

	public abstract void execute(XmlPopulatorContext xmlPopulatorContext, Node node);

	
}
