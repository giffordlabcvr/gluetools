/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.collation.populating.xml;

import java.util.ArrayList;
import java.util.List;

import javax.xml.xpath.XPathExpression;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
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

	@Override
	public void validate(CommandContext cmdContext) {
		childRules.forEach(rule -> {
			rule.validate(cmdContext);
		});
	}

	@Override
	public List<String> updatablePropertyPaths() {
		List<String> paths = new ArrayList<String>();
		childRules.stream().forEach(rule -> paths.addAll(rule.updatablePropertyPaths()));
		return paths;
	}

	
	
}
