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

import java.util.Arrays;
import java.util.List;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.core.translation.TranslationUtils;
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
		return selectAlignmentColumns(cmdContext, relRefName, getFeatureName(), startCodon, endCodon);
	}

	public static List<ReferenceSegment> selectAlignmentColumns(CommandContext cmdContext, String relRefName, String featureName, String startCodon, String endCodon) {
		FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(relRefName, featureName));
		List<ReferenceSegment> featureRefSegs = featureLoc.segmentsAsReferenceSegments();
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
		int lcRegionNtStart = startLabeledCodon.getNtStart();
		int lcRegionNtEnd = endLabeledCodon.getNtStart()+2;
		return ReferenceSegment
				.intersection(featureRefSegs, Arrays.asList(new ReferenceSegment(lcRegionNtStart, lcRegionNtEnd)), ReferenceSegment.cloneLeftSegMerger());

	}

	public String generateAminoAcidAlmtRowString(CommandContext cmdContext, ReferenceSequence relatedRef,
			Translator translator, List<ReferenceSegment> featureRefSegs, 
			ReferenceSegment minMaxSeg, AlignmentMember almtMember) {
		
		FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, FeatureLocation.pkMap(relatedRef.getName(), getFeatureName()));
		int codon1Start = featureLoc.getCodon1Start(cmdContext);


		List<QueryAlignedSegment> memberQaSegs = almtMember.segmentsAsQueryAlignedSegments();
		Alignment tipAlmt = almtMember.getAlignment();
		memberQaSegs = tipAlmt.translateToRelatedRef(cmdContext, memberQaSegs, relatedRef);
		memberQaSegs = ReferenceSegment.intersection(memberQaSegs, featureRefSegs, ReferenceSegment.cloneLeftSegMerger());
		
		// important to merge abutting here otherwise you may get gaps if the boundary is within a codon.
		memberQaSegs = QueryAlignedSegment.mergeAbutting(memberQaSegs, 
				QueryAlignedSegment.mergeAbuttingFunctionQueryAlignedSegment(), 
				QueryAlignedSegment.abutsPredicateQueryAlignedSegment());

		
		memberQaSegs = TranslationUtils.truncateToCodonAligned(codon1Start, memberQaSegs);
		AbstractSequenceObject seqObj = almtMember.getSequence().getSequenceObject();
		String memberNTs = seqObj.getNucleotides(cmdContext);
		StringBuffer alignmentRow = new StringBuffer();
		int ntIndex = minMaxSeg.getRefStart();
		for(QueryAlignedSegment seg: memberQaSegs) {
			while(ntIndex < seg.getRefStart()) {
				alignmentRow.append("-");
				ntIndex += 3;
			}
			String segNTs = SegmentUtils.base1SubString(memberNTs, seg.getQueryStart(), seg.getQueryEnd());
			String segAAs = translator.translateToAaString(segNTs);
			alignmentRow.append(segAAs);
			ntIndex = seg.getRefEnd()+1;
		}
		while(ntIndex <= minMaxSeg.getRefEnd()) {
			alignmentRow.append("-");
			ntIndex += 3;
		}
		return alignmentRow.toString();
	}
	
	public void setStartCodon(String startCodon) {
		this.startCodon = startCodon;
	}

	public void setEndCodon(String endCodon) {
		this.endCodon = endCodon;
	}

	
	
}
