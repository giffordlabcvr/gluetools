package uk.ac.gla.cvr.gluetools.core.datafield.populator;

import java.util.List;

import javax.xml.xpath.XPathExpression;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import uk.ac.gla.cvr.gluetools.core.collation.sequence.CollatedSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.XmlUtils;

public abstract class NodeSelectorRule extends PopulatorRule {

	
	private XPathExpression xPathExpression;
	private List<PopulatorRule> childRules;
	

	protected void setXPathExpression(XPathExpression xPathExpresison) {
		this.xPathExpression = xPathExpresison;
	}
	
	protected XPathExpression getXPathExpression() {
		return xPathExpression;
	}

	protected Node selectNode(Node node) {
		Node selectedNode;
		try {
			selectedNode = XmlUtils.getXPathNode(node, getXPathExpression());
		} catch (Exception e) {
			throw new DataFieldPopulatorException(e, DataFieldPopulatorException.Code.POPULATOR_RULE_FAILED, e.getLocalizedMessage());
		}
		return selectedNode;
	}

	protected List<PopulatorRule> getChildRules() {
		return childRules;
	}

	public final void configure(Element configElem) {
		configureChildRules(configElem);
		configureLocal(configElem);
	}
	
	/**
	 * Configure the local properties of this rule, i.e. not the child rules. 
	 */
	protected abstract void configureLocal(Element configElem);
	
	private void configureChildRules(Element configElem) {
		PopulatorRuleFactory populatorRuleFactory = PluginFactory.get(PopulatorRuleFactory.creator);
		String alternateElemsXPath = XmlUtils.alternateElemsXPath(populatorRuleFactory.getElementNames());
		List<Element> ruleElems = PluginUtils.findConfigElements(configElem, alternateElemsXPath);
		childRules = populatorRuleFactory.createFromElements(ruleElems);
	}

	protected void executeChildRules(CollatedSequence collatedSequence, Node selectedNode) {
		if(selectedNode != null) {
			childRules.forEach(rule -> {
				try {
					rule.execute(collatedSequence, selectedNode);
				} catch(Exception e) {
					throw new DataFieldPopulatorException(e, DataFieldPopulatorException.Code.POPULATOR_CHILD_RULE_FAILED, e.getLocalizedMessage());
				}
			});
		}
	}

	public final void execute(CollatedSequence collatedSequence, Node node) {
		Node selectedNode = selectNode(node);
		executeChildRules(collatedSequence, selectedNode);
	}

}
