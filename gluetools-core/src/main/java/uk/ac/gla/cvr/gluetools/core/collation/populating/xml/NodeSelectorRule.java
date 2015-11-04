package uk.ac.gla.cvr.gluetools.core.collation.populating.xml;

import java.util.List;

import javax.xml.xpath.XPathExpression;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public abstract class NodeSelectorRule extends XmlPopulatorRule {

	
	private XPathExpression xPathExpression;
	private List<XmlPopulatorRule> childRules;
	

	protected void setXPathExpression(XPathExpression xPathExpresison) {
		this.xPathExpression = xPathExpresison;
	}
	
	protected XPathExpression getXPathExpression() {
		return xPathExpression;
	}

	private List<Node> selectNodes(Node node) {
		List<Node> selectedNodes;
		try {
			selectedNodes = GlueXmlUtils.getXPathNodes(node, getXPathExpression());
		} catch (Exception e) {
			throw new XmlPopulatorException(e, XmlPopulatorException.Code.POPULATOR_RULE_FAILED, e.getLocalizedMessage());
		}
		return selectedNodes;
	}

	protected List<XmlPopulatorRule> getChildRules() {
		return childRules;
	}

	public final void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		configureChildRules(pluginConfigContext, configElem);
		configureLocal(pluginConfigContext, configElem);
	}
	
	/**
	 * Configure the local properties of this rule, i.e. not the child rules. 
	 */
	protected abstract void configureLocal(PluginConfigContext pluginConfigContext, Element configElem);
	
	private void configureChildRules(PluginConfigContext pluginConfigContext, Element configElem) {
		XmlPopulatorRuleFactory populatorRuleFactory = getRuleFactory();
		String alternateElemsXPath = GlueXmlUtils.alternateElemsXPath(populatorRuleFactory.getElementNames());
		List<Element> ruleElems = PluginUtils.findConfigElements(configElem, alternateElemsXPath);
		childRules = populatorRuleFactory.createFromElements(pluginConfigContext, ruleElems);
	}

	protected void executeChildRules(XmlPopulatorContext xmlPopulatorContext, Node selectedNode) {
		childRules.forEach(rule -> {
			try {
				rule.execute(xmlPopulatorContext, selectedNode);
			} catch(Exception e) {
				throw new XmlPopulatorException(e, XmlPopulatorException.Code.POPULATOR_CHILD_RULE_FAILED, e.getLocalizedMessage());
			}
		});
	}

	public final void execute(XmlPopulatorContext xmlPopulatorContext, Node node) {
		List<Node> selectedNodes = selectNodes(node);
		selectedNodes.forEach(selectedNode ->
			executeChildRules(xmlPopulatorContext, selectedNode));
	}

}
