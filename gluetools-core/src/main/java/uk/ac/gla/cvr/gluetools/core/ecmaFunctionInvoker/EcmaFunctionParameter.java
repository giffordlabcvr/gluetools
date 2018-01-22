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
package uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.paramCompleter.EcmaFunctionParamCompleter;
import uk.ac.gla.cvr.gluetools.core.ecmaFunctionInvoker.paramCompleter.EcmaFunctionParamCompleterFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@PluginClass(elemName="parameter")
public class EcmaFunctionParameter implements Plugin {

	public static final String NAME = "name";

	private String name;
	private EcmaFunctionParamCompleter paramCompleter = null;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		name = PluginUtils.configureStringProperty(configElem, NAME, true);
		EcmaFunctionParamCompleterFactory paramCompleterFactory = PluginFactory.get(EcmaFunctionParamCompleterFactory.creator);
		String alternateElemsXPath = GlueXmlUtils.alternateElemsXPath(paramCompleterFactory.getElementNames());
		Element paramCompleterElem = PluginUtils.findConfigElement(configElem, alternateElemsXPath, false);
		if(paramCompleterElem != null) {
			paramCompleter = paramCompleterFactory.createFromElement(pluginConfigContext, paramCompleterElem);
		}
	}

	public String getName() {
		return name;
	}

	public EcmaFunctionParamCompleter getParamCompleter() {
		return paramCompleter;
	}
	
}
