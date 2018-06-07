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

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.IsoCountryMatcherConverter;
import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.MatcherConverter;
import uk.ac.gla.cvr.gluetools.core.collation.populating.regex.RegexExtractorFormatter;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.IsoCountryUtils;
import uk.ac.gla.cvr.gluetools.utils.IsoCountryUtils.CodeStyle;

@PluginClass(elemName="isoCountryPropertyPopulator")
public class IsoCountryPropertyPopulatorRule extends BaseXmlPropertyPopulatorRule {

	private CodeStyle codeStyle;

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem)  {
		super.configure(pluginConfigContext, configElem);
		this.codeStyle = PluginUtils.configureEnum(CodeStyle.class, configElem, "@codeStyle", true);
		populateProperty(pluginConfigContext, configElem);
		
		List<MatcherConverter> valueConverters = new ArrayList<MatcherConverter>();
		valueConverters.addAll(PluginFactory.createPlugins(pluginConfigContext, RegexExtractorFormatter.class, 
				PluginUtils.findConfigElements(configElem, "valueConverter")));
		valueConverters.add(new IsoCountryMatcherConverter(codeStyle));
		setValueConverters(valueConverters);
		setMainExtractor(null);
	}

	protected void populateProperty(PluginConfigContext pluginConfigContext, Element configElem) {
		setProperty(PluginUtils.configureString(configElem, "@property", true));
	}
}
