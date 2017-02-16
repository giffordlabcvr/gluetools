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
