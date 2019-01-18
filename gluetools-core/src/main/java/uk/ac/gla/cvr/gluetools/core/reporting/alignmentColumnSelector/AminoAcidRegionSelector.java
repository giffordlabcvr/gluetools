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
import java.util.LinkedHashSet;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodonReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledQueryAminoAcid;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.NucleotideContentProvider;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.translation.Translator;

@PluginClass(elemName="aminoAcidRegionSelector")
public class AminoAcidRegionSelector extends RegionSelector {

	private String startCodon;
	private String endCodon;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.startCodon = PluginUtils.configureStringProperty(configElem, "startCodon", false);
		this.endCodon = PluginUtils.configureStringProperty(configElem, "endCodon", false);
	}

	@Override
	protected List<ReferenceSegment> selectAlignmentColumnsInternal(CommandContext cmdContext, String relRefName) {
		List<LabeledCodon> selectedLabeledCodons = selectLabeledCodons(cmdContext, relRefName);
		return refSegsForSelectedLabeledCodons(selectedLabeledCodons);
		
	}

	private List<ReferenceSegment> refSegsForSelectedLabeledCodons(
			List<LabeledCodon> selectedLabeledCodons) {
		List<ReferenceSegment> selectedRefSegs = new ArrayList<ReferenceSegment>();
		for(LabeledCodon selectedLabeledCodon: selectedLabeledCodons) {
			List<LabeledCodonReferenceSegment> lcRefSegments = selectedLabeledCodon.getLcRefSegments();
			lcRefSegments.forEach(lcRefSeg -> {
				selectedRefSegs.add(new ReferenceSegment(lcRefSeg.getRefStart(), lcRefSeg.getRefEnd()));
			});
		}
		return ReferenceSegment.mergeAbutting(selectedRefSegs, 
				ReferenceSegment.mergeAbuttingFunctionReferenceSegment(), 
				ReferenceSegment.abutsPredicateReferenceSegment());
	}

	public void setStartCodon(String startCodon) {
		this.startCodon = startCodon;
	}

	public void setEndCodon(String endCodon) {
		this.endCodon = endCodon;
	}

	// TODO -- logic needs to be moved out of AARegionSelector and into calling commands.
	// which should rely on AARegionSelector.translateQueryNucleotides
	public List<LabeledQueryAminoAcid> generateAminoAcidAlmtRow(
			CommandContext cmdContext, ReferenceSequence relatedRef,
			Translator translator, AlignmentMember almtMember) {
		FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(relatedRef.getName(), getFeatureName()));
		List<QueryAlignedSegment> memberToAlmtSegs = almtMember.segmentsAsQueryAlignedSegments();
		List<QueryAlignedSegment> memberToRelatedRef = almtMember.getAlignment().translateToRelatedRef(cmdContext, memberToAlmtSegs, relatedRef);
		List<ReferenceSegment> selectedRefSegs = refSegsForSelectedLabeledCodons(selectLabeledCodons(cmdContext, relatedRef.getName()));
		memberToRelatedRef = ReferenceSegment.intersection(memberToRelatedRef, selectedRefSegs, QueryAlignedSegment.cloneLeftSegMerger());
		return featureLoc.translateQueryNucleotides(cmdContext, translator, memberToRelatedRef, almtMember.getSequence().getSequenceObject());
	}

	public List<LabeledCodon> selectLabeledCodons(CommandContext cmdContext, String relatedRefName) {
		FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(relatedRefName, getFeatureName()));
		featureLoc.getFeature().checkCodesAminoAcids();
		LabeledCodon startLabeledCodon;
		if(startCodon != null) {
			startLabeledCodon = featureLoc.getLabeledCodon(cmdContext, startCodon);
		} else {
			startLabeledCodon = featureLoc.getLabeledCodons(cmdContext).get(0); 
		}
		LabeledCodon endLabeledCodon;
		if(endCodon != null) {
			endLabeledCodon = featureLoc.getLabeledCodon(cmdContext, endCodon);
		} else {
			List<LabeledCodon> labeledCodons = featureLoc.getLabeledCodons(cmdContext);
			endLabeledCodon = labeledCodons.get(labeledCodons.size()-1); 
		}
		if(endLabeledCodon.getNtStart() < startLabeledCodon.getNtStart()) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Codon with label \""+endCodon+"\" occurs before codon with label \""+startCodon+"\"");
		}
		List<LabeledCodon> selectedLabeledCodons = featureLoc.getLabeledCodons(cmdContext, startLabeledCodon, endLabeledCodon);
		List<RegionSelector> excludeRegionSelectors = getExcludeRegionSelectors();
		if(excludeRegionSelectors != null && !excludeRegionSelectors.isEmpty()) {
			LinkedHashSet<LabeledCodon> selectedLabeledCodonSet = new LinkedHashSet<LabeledCodon>(selectedLabeledCodons);
			for(RegionSelector excludeRegionSelector: excludeRegionSelectors) {
				AminoAcidRegionSelector aaExcludeRegionSelector = (AminoAcidRegionSelector) excludeRegionSelector;
				selectedLabeledCodonSet.removeAll(aaExcludeRegionSelector.selectLabeledCodons(cmdContext, relatedRefName));
			}
			selectedLabeledCodons = new ArrayList<LabeledCodon>(selectedLabeledCodonSet);
		}
		return selectedLabeledCodons;
	}

	
	

	@Override
	public void validate(CommandContext cmdContext, String relRefName) {
		super.validate(cmdContext, relRefName);
		Feature referredToFeature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(this.getFeatureName()));
		if(!referredToFeature.codesAminoAcids()) {
			throw new AlignmentColumnsSelectorException(AlignmentColumnsSelectorException.Code.INVALID_SELECTOR, 
					"Amino acid region selector refers to feature "+referredToFeature.getName()+" which is not an amino acid coding feature");
		}
		FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(relRefName, getFeatureName()));
		LabeledCodon startLabeledCodon;
		if(startCodon != null) {
			startLabeledCodon = featureLoc.getLabeledCodon(cmdContext, startCodon);
		} else {
			startLabeledCodon = featureLoc.getLabeledCodons(cmdContext).get(0); 
		}
		if(startLabeledCodon == null) {
			throw new AlignmentColumnsSelectorException(AlignmentColumnsSelectorException.Code.INVALID_SELECTOR, 
					"Amino acid region selector refers to unknown start codon "+startLabeledCodon);
		}
		LabeledCodon endLabeledCodon;
		if(endCodon != null) {
			endLabeledCodon = featureLoc.getLabeledCodon(cmdContext, endCodon);
		} else {
			List<LabeledCodon> labeledCodons = featureLoc.getLabeledCodons(cmdContext);
			endLabeledCodon = labeledCodons.get(labeledCodons.size()-1); 
		}
		if(endLabeledCodon == null) {
			throw new AlignmentColumnsSelectorException(AlignmentColumnsSelectorException.Code.INVALID_SELECTOR, 
					"Amino acid region selector refers to unknown end codon "+endLabeledCodon);
		}

		
		
		
	}

	public List<LabeledQueryAminoAcid> translateQueryNucleotides(CommandContext cmdContext,
			String relRefName, List<QueryAlignedSegment> queryToRefSegs, Translator translator, NucleotideContentProvider queryNucleotideContent) {
		FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(relRefName, getFeatureName()));
		List<ReferenceSegment> refSegs = selectAlignmentColumns(cmdContext, relRefName);
		List<QueryAlignedSegment> queryToRefSegsInRegion = ReferenceSegment.intersection(refSegs, queryToRefSegs, ReferenceSegment.cloneRightSegMerger());
		return featureLoc.translateQueryNucleotides(cmdContext, translator, queryToRefSegsInRegion, queryNucleotideContent);
	}

	
	
}
