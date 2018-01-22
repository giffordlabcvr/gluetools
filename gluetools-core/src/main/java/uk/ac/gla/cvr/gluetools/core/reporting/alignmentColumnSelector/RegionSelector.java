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
package uk.ac.gla.cvr.gluetools.core.reporting.alignmentColumnSelector;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public abstract class RegionSelector implements Plugin {

	private List<RegionSelector> excludeRegionSelectors;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		RegionSelectorFactory regionSelectorFactory = PluginFactory.get(RegionSelectorFactory.creator);
		Element excludeRegionsElem = PluginUtils.findConfigElement(configElem, "excludeRegions");
		if(excludeRegionsElem != null) {
			String alternateElemsXPath = GlueXmlUtils.alternateElemsXPath(regionSelectorFactory.getElementNames());
			List<Element> ruleElems = PluginUtils.findConfigElements(excludeRegionsElem, alternateElemsXPath);
			this.excludeRegionSelectors = regionSelectorFactory.createFromElements(pluginConfigContext, ruleElems);
		}
	}

	public final List<ReferenceSegment> selectAlignmentColumns(CommandContext cmdContext, String relRefName) {
		List<ReferenceSegment> refSegs = selectAlignmentColumnsInternal(cmdContext, relRefName);
		if(this.excludeRegionSelectors != null) {
			for(RegionSelector excludeRegionSelector: excludeRegionSelectors) {
				List<ReferenceSegment> excludeRefSegs = excludeRegionSelector.selectAlignmentColumns(cmdContext, relRefName);
				refSegs = ReferenceSegment.subtract(refSegs, excludeRefSegs);
			}
		}
		return refSegs;
	}

	
	
	protected abstract List<ReferenceSegment> selectAlignmentColumnsInternal(CommandContext cmdContext, String relRefName);

}
