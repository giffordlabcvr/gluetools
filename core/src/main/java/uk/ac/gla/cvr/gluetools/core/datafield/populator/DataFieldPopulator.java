package uk.ac.gla.cvr.gluetools.core.datafield.populator;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.sequence.CollatedSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


/**
 * A plugin which contains a set of rules to populate the data fields of a collated sequence.
 */
public class DataFieldPopulator implements Plugin {

	public static String ELEM_NAME = "dataFieldPopulator";
	private List<DataFieldPopulatorRule> rules;
	
	protected List<DataFieldPopulatorRule> getRules() {
		return rules;
	}
	
	@Override
	public void configure(Element configElem) {
		List<Element> ruleElems = PluginUtils.findConfigElements(configElem, "rules/*");
		rules = PluginFactory.get(DataFieldPopulatorRuleFactory.creator).createFromElements(ruleElems);
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
