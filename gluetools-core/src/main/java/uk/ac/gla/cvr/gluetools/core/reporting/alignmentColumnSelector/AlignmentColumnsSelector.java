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

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.FeatureReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment.IAminoAcidAlignmentColumnsSelector;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter.SimpleDataObjectNameInstantiator;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.NucleotideContentProvider;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigException;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.translation.CommandContextTranslator;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;
import uk.ac.gla.cvr.gluetools.utils.GlueXmlUtils;

@PluginClass(elemName="alignmentColumnsSelector",
	description="Filter applied to an alignment which selects columns based on feature locations or specific coordinates")
public class AlignmentColumnsSelector extends ModulePlugin<AlignmentColumnsSelector> implements IAminoAcidAlignmentColumnsSelector {

	private String relRefName;
	
	private List<RegionSelector> regionSelectors = new ArrayList<RegionSelector>();
	
	public AlignmentColumnsSelector() {
		super();
		addSimplePropertyName("relRefName", new SimpleDataObjectNameInstantiator(ReferenceSequence.class, ReferenceSequence.NAME_PROPERTY));
		registerModuleDocumentCmdClass(AddRegionSelectorCommand.class);
	}
	
	public void setRelRefName(String relRefName) {
		this.relRefName = relRefName;
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.relRefName = PluginUtils.configureStringProperty(configElem, "relRefName", true);
		RegionSelectorFactory regionSelectorFactory = PluginFactory.get(RegionSelectorFactory.creator);
		String alternateElemsXPath = GlueXmlUtils.alternateElemsXPath(regionSelectorFactory.getElementNames());
		List<Element> regionSelectorElems = PluginUtils.findConfigElements(configElem, alternateElemsXPath);
		this.regionSelectors = regionSelectorFactory.createFromElements(pluginConfigContext, regionSelectorElems);
		if(this.regionSelectors.size() < 1) {
			throw new PluginConfigException(PluginConfigException.Code.TOO_FEW_CONFIG_ELEMENTS, alternateElemsXPath, regionSelectors.size(), 1);
		}
	}



	
	@Override
	public List<FeatureReferenceSegment> selectAlignmentColumns(Alignment alignment ,CommandContext cmdContext) {
		RegionSelector firstRegionSelector = this.regionSelectors.get(0);
		List<FeatureReferenceSegment> refSegs = firstRegionSelector.selectAlignmentColumns(cmdContext, getRelatedRefName());
	
		for(int i = 1; i < this.regionSelectors.size(); i++) {
			RegionSelector nextRegionSelector = this.regionSelectors.get(i);
			List<FeatureReferenceSegment> nextRefSegs = nextRegionSelector.selectAlignmentColumns(cmdContext, getRelatedRefName());
			// maybe we could make a union function out of the below code?
			List<FeatureReferenceSegment> intersection = ReferenceSegment.intersection(refSegs, nextRefSegs, ReferenceSegment.cloneRightSegMerger());
			refSegs = ReferenceSegment.subtract(refSegs, intersection);
			refSegs.addAll(nextRefSegs);
			ReferenceSegment.sortByRefStart(refSegs);
			refSegs = ReferenceSegment.mergeAbutting(refSegs, 
					FeatureReferenceSegment.mergeAbuttingFunctionFeatureReferenceSegment(), 
					FeatureReferenceSegment.abutsPredicateFeatureReferenceSegment());
		}
		
		return refSegs;
	}

	public void addRegionSelector(RegionSelector regionSelector) {
		regionSelectors.add(regionSelector);
	}
	
	@Override
	public String getRelatedRefName() {
		return relRefName;
	}

	@Override
	public void checkAminoAcidSelector(CommandContext cmdContext) {
		for(RegionSelector regionSelector: this.regionSelectors) {
			regionSelector.checkAminoAcidSelector(cmdContext);
		}
	}

	@Override
	public List<LabeledCodon> selectLabeledCodons(CommandContext cmdContext) {
		List<LabeledCodon> selectedLabeledCodons = new ArrayList<LabeledCodon>();
		String relatedRefName = getRelatedRefName();
		for(RegionSelector regionSelector: this.regionSelectors) {
			AminoAcidRegionSelector aaRegionSelector = (AminoAcidRegionSelector) regionSelector;
			selectedLabeledCodons.addAll(aaRegionSelector.selectLabeledCodons(cmdContext, relatedRefName));
		}
		return selectedLabeledCodons;
	}

	// TODO -- logic needs to be moved out of AARegionSelector and into calling commands.
	// which should rely on AARegionSelector.translateQueryNucleotides
	@Override
	public List<LabeledQueryAminoAcid> generateAminoAcidAlmtRow(
			CommandContext cmdContext, Alignment alignment,
			AlignmentMember almtMember) {
		List<LabeledQueryAminoAcid> lqaas = new ArrayList<LabeledQueryAminoAcid>();
		ReferenceSequence relatedRef = alignment.getRelatedRef(cmdContext, getRelatedRefName());
		Translator translator = new CommandContextTranslator(cmdContext);
		for(RegionSelector regionSelector: this.regionSelectors) {
			AminoAcidRegionSelector aaRegionSelector = (AminoAcidRegionSelector) regionSelector;
			lqaas.addAll(aaRegionSelector.generateAminoAcidAlmtRow(cmdContext, relatedRef, translator, almtMember));
		}
		return lqaas;
	}

	@Override
	public void validate(CommandContext cmdContext) {
		super.validate(cmdContext);
		for(RegionSelector regionSelector: this.regionSelectors) {
			regionSelector.validate(cmdContext, relRefName);
		}
	}

	@Override
	public List<LabeledQueryAminoAcid> translateQueryNucleotides(CommandContext cmdContext, Translator translator,
			List<QueryAlignedSegment> queryToRefSegs, NucleotideContentProvider queryNucleotideContent) {
		List<LabeledQueryAminoAcid> lqaas = new ArrayList<LabeledQueryAminoAcid>();
		for(RegionSelector regionSelector: this.regionSelectors) {
			AminoAcidRegionSelector aaRegionSelector = (AminoAcidRegionSelector) regionSelector;
			lqaas.addAll(aaRegionSelector.translateQueryNucleotides(cmdContext, getRelatedRefName(), queryToRefSegs, translator, queryNucleotideContent));
		}
		return lqaas;
	}
}
