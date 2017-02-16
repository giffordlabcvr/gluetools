package uk.ac.gla.cvr.gluetools.core.reporting.alignmentColumnSelector;

import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.IAlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@PluginClass(elemName="alignmentColumnsSelector")
public class AlignmentColumnsSelector extends ModulePlugin<AlignmentColumnsSelector> implements IAlignmentColumnsSelector {

	private String relRefName;
	
	private List<RegionSelector> regionSelectors;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.relRefName = PluginUtils.configureStringProperty(configElem, "relRefName", true);
		RegionSelectorFactory regionSelectorFactory = PluginFactory.get(RegionSelectorFactory.creator);
		String alternateElemsXPath = GlueXmlUtils.alternateElemsXPath(regionSelectorFactory.getElementNames());
		List<Element> ruleElems = PluginUtils.findConfigElements(configElem, alternateElemsXPath);
		this.regionSelectors = regionSelectorFactory.createFromElements(pluginConfigContext, ruleElems);
		if(this.regionSelectors.size() < 1) {
			throw new PluginConfigException(PluginConfigException.Code.TOO_FEW_CONFIG_ELEMENTS, alternateElemsXPath, regionSelectors.size(), 1);
		}
	}

	@Override
	public List<ReferenceSegment> selectAlignmentColumns(CommandContext cmdContext) {
		RegionSelector firstRegionSelector = this.regionSelectors.get(0);
		List<ReferenceSegment> refSegs = firstRegionSelector.selectAlignmentColumns(cmdContext, getRelatedRefName());
	
		for(int i = 1; i < this.regionSelectors.size(); i++) {
			RegionSelector nextRegionSelector = this.regionSelectors.get(i);
			List<ReferenceSegment> nextRefSegs = nextRegionSelector.selectAlignmentColumns(cmdContext, getRelatedRefName());
			// maybe we could make a union function out of the below code?
			List<ReferenceSegment> intersection = ReferenceSegment.intersection(refSegs, nextRefSegs, ReferenceSegment.cloneRightSegMerger());
			refSegs = ReferenceSegment.subtract(refSegs, intersection);
			refSegs.addAll(nextRefSegs);
			ReferenceSegment.sortByRefStart(refSegs);
			ReferenceSegment.mergeAbutting(refSegs, ReferenceSegment.mergeAbuttingFunctionReferenceSegment(), ReferenceSegment.abutsPredicateReferenceSegment());
		}
		
		return refSegs;
	}

	@Override
	public String getRelatedRefName() {
		return relRefName;
	}

	
	
}
