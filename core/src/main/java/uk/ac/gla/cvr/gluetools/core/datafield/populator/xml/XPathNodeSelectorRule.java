package uk.ac.gla.cvr.gluetools.core.datafield.populator.xml;

import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import uk.ac.gla.cvr.gluetools.core.collation.sequence.CollatedSequence;
import uk.ac.gla.cvr.gluetools.core.datafield.populator.DataFieldPopulatorException;
import uk.ac.gla.cvr.gluetools.core.datafield.populator.DataFieldPopulatorRule;
import uk.ac.gla.cvr.gluetools.core.datafield.populator.DataFieldPopulatorRuleFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public class XPathNodeSelectorRule extends XPathPopulatorRule implements Plugin {

	public static String ELEM_NAME = "nodeSelector";
	
	private List<DataFieldPopulatorRule> rules;
	
	protected List<DataFieldPopulatorRule> getRules() {
		return rules;
	}
	
	@Override
	public void configure(Element configElem) {
		super.configure(configElem);
		List<Element> ruleElems = PluginUtils.findConfigElements(configElem, "rules/*");
		rules = PluginFactory.get(DataFieldPopulatorRuleFactory.creator).createFromElements(ruleElems);
	}
	
	public void execute(CollatedSequence collatedSequence, Node node) {
		Node selectedNode;
		try {
			selectedNode = XmlUtils.getXPathNode(node, getXPathExpression());
		} catch (Exception e) {
			throw new DataFieldPopulatorException(e, DataFieldPopulatorException.Code.POPULATOR_RULE_FAILED, e.getLocalizedMessage());
		}
		if(selectedNode != null) {
			rules.forEach(rule -> {
				try {
					rule.execute(collatedSequence, selectedNode);
				} catch(Exception e) {
					throw new DataFieldPopulatorException(e, DataFieldPopulatorException.Code.POPULATOR_CHILD_RULE_FAILED, e.getLocalizedMessage());
				}
			});
		}
	}
	
}
