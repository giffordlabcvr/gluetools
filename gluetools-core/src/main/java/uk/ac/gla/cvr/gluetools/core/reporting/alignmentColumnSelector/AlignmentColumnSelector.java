package uk.ac.gla.cvr.gluetools.core.reporting.alignmentColumnSelector;

import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.collation.populating.genbank.GenbankXmlPopulatorRuleFactory;
import uk.ac.gla.cvr.gluetools.core.collation.populating.xml.XmlPopulatorRuleFactory;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@PluginClass(elemName="alignmentColumnSelector")
public class AlignmentColumnSelector extends ModulePlugin<AlignmentColumnSelector> {

	private List<RegionSelector> regionSelectors;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		RegionSelectorFactory regionSelectorFactory = PluginFactory.get(RegionSelectorFactory.creator);
		String alternateElemsXPath = GlueXmlUtils.alternateElemsXPath(regionSelectorFactory.getElementNames());
		List<Element> ruleElems = PluginUtils.findConfigElements(configElem, alternateElemsXPath);
		this.regionSelectors = regionSelectorFactory.createFromElements(pluginConfigContext, ruleElems);
	}

}
