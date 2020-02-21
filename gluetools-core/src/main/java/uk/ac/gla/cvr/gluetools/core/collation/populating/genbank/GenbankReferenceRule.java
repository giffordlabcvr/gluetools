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
package uk.ac.gla.cvr.gluetools.core.collation.populating.genbank;

import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.NodeSelectorRule;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException.Code;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@PluginClass(elemName="gbReference")
public class GenbankReferenceRule extends NodeSelectorRule {


	@Override
	public void configureLocal(PluginConfigContext pluginConfigContext, Element configElem) {
		String allowMultipleXPath = "@allowMultiple";
		boolean allowMultiple = Boolean.parseBoolean(PluginUtils.configureString(configElem, allowMultipleXPath, "false"));
		if(allowMultiple) {
			String xPathString = "/GBSeq/GBSeq_references/GBReference";
			try {
				setXPathExpression(GlueXmlUtils.createXPathEngine().compile(xPathString));
			} catch (XPathExpressionException xpee) {
				throw new PluginConfigException(xpee, Code.CONFIG_FORMAT_ERROR, xPathString, xpee.getLocalizedMessage(), xPathString);
			}
		} else {
			String refNumberXPath = "@refNumber";
			String refNumberString = PluginUtils.configureString(configElem, refNumberXPath, "1");
			String xPathString = "/GBSeq/GBSeq_references/GBReference[GBReference_reference='"+refNumberString+"']";
			try {
				setXPathExpression(GlueXmlUtils.createXPathEngine().compile(xPathString));
			} catch (XPathExpressionException xpee) {
				throw new PluginConfigException(xpee, Code.CONFIG_FORMAT_ERROR, refNumberXPath, xpee.getLocalizedMessage(), refNumberString);
			}
		}
	}

}
