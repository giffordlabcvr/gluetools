package uk.ac.gla.cvr.gluetools.core.datafield.populator;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.sequence.CollatedSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;


/**
 * A plugin which contains a set of rules to populate the data fields of a collated sequence.
 */
@PluginClass(elemName="dataFieldPopulator")
public class DataFieldPopulator implements Plugin {

	private List<PopulatorRule> rules;
	
	protected List<PopulatorRule> getRules() {
		return rules;
	}
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		PopulatorRuleFactory populatorRuleFactory = PluginFactory.get(PopulatorRuleFactory.creator);
		String alternateElemsXPath = XmlUtils.alternateElemsXPath(populatorRuleFactory.getElementNames());
		List<Element> ruleElems = PluginUtils.findConfigElements(configElem, alternateElemsXPath);
		rules = populatorRuleFactory.createFromElements(pluginConfigContext, ruleElems);
	}

	/**
	 * Set values for zero or more data fields of the given sequence.
	 */
	public void populate(CollatedSequence collatedSequence) {
		rules.forEach(rule -> {
			rule.execute(collatedSequence, collatedSequence.asXml());
		});
	}
}
