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

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FeatureReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

public abstract class RegionSelector implements Plugin {

	private List<RegionSelector> excludeRegionSelectors;
	private String featureName;

	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		RegionSelectorFactory regionSelectorFactory = PluginFactory.get(RegionSelectorFactory.creator);
		Element excludeRegionsElem = PluginUtils.findConfigElement(configElem, "excludeRegions");
		if(excludeRegionsElem != null) {
			String alternateElemsXPath = GlueXmlUtils.alternateElemsXPath(regionSelectorFactory.getElementNames());
			List<Element> ruleElems = PluginUtils.findConfigElements(excludeRegionsElem, alternateElemsXPath);
			this.excludeRegionSelectors = regionSelectorFactory.createFromElements(pluginConfigContext, ruleElems);
		}
		this.featureName = PluginUtils.configureStringProperty(configElem, "featureName", true);
	}

	public final List<FeatureReferenceSegment> selectAlignmentColumns(CommandContext cmdContext, String relRefName) {
		List<FeatureReferenceSegment> refSegs = selectAlignmentColumnsInternal(cmdContext, relRefName);
		if(this.excludeRegionSelectors != null) {
			for(RegionSelector excludeRegionSelector: excludeRegionSelectors) {
				List<FeatureReferenceSegment> excludeRefSegs = excludeRegionSelector.selectAlignmentColumns(cmdContext, relRefName);
				refSegs = ReferenceSegment.subtract(refSegs, excludeRefSegs);
			}
		}
		return refSegs;
	}

	protected List<RegionSelector> getExcludeRegionSelectors() {
		return excludeRegionSelectors;
	}

	protected abstract List<FeatureReferenceSegment> selectAlignmentColumnsInternal(CommandContext cmdContext, String relRefName);


	public void checkWithinParentFeature(CommandContext cmdContext, Feature parentFeature) {
		Feature referredToFeature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(this.featureName));
		if((!referredToFeature.getName().equals(parentFeature.getName())) && !referredToFeature.isDescendentOf(parentFeature)) {
			throw new AlignmentColumnsSelectorException(AlignmentColumnsSelectorException.Code.INVALID_SELECTOR, 
					"Region selector refers to feature "+referredToFeature.getName()+" which is not the same feature, or a descendent of "+parentFeature.getName());
		}
		List<RegionSelector> excludeRegionSelectors = getExcludeRegionSelectors();
		if(excludeRegionSelectors != null) {
			excludeRegionSelectors.forEach(ers -> ers.checkWithinParentFeature(cmdContext, parentFeature));
		}
	}

	public final void checkAminoAcidSelector(CommandContext cmdContext) {
		if(!(this instanceof AminoAcidRegionSelector)) {
			throw new AlignmentColumnsSelectorException(AlignmentColumnsSelectorException.Code.INVALID_SELECTOR, 
					"Alingment columns selector contains non-amino-acid region selector");
		}
		List<RegionSelector> excludeRegionSelectors = getExcludeRegionSelectors();
		if(excludeRegionSelectors != null) {
			excludeRegionSelectors.forEach(ers -> ers.checkAminoAcidSelector(cmdContext));
		}
	}
	public String getFeatureName() {
		return featureName;
	}

	public void setFeatureName(String featureName) {
		this.featureName = featureName;
	}

	public void validate(CommandContext cmdContext, String relRefName) {
		List<RegionSelector> excludeRegionSelectors = getExcludeRegionSelectors();
		if(excludeRegionSelectors != null) {
			excludeRegionSelectors.forEach(ers -> ers.validate(cmdContext, relRefName));
		}
	}

}
