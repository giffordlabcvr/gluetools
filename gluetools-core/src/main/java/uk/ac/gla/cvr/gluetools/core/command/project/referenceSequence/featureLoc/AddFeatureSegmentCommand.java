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
package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodonReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc.FeatureSegmentException.Code;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.feature.Feature;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureLoc.FeatureLocation;
import uk.ac.gla.cvr.gluetools.core.datamodel.featureSegment.FeatureSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.translationModification.TranslationModifier;

@CommandClass( 
	commandWords={"add","segment"}, 
	docoptUsages={"( <refStart> <refEnd> | -l <lcStart> <lcEnd> ) [ -t <translationModifier> ] [ -s <spliceIndex> ] [ -r <transcriptionIndex> ]"},
	docoptOptions={
		"-l, --labeledCodon                               Set location based on labeled codons",
		"-t, --translationModifier <translationModifier>  Translation modifier module name",
		"-s, --spliceIndex <spliceIndex>                  Splice index",
		"-r, --transcriptionIndex <transcriptionIndex>    Transcription index"},
	metaTags = {},
	description="Add a new segment of the reference sequence", 
	furtherHelp="The segment endpoints can be set in different ways. "+ 
			"If <refStart> and <refEnd> are used these define simply the nucleotide region of the feature location's reference sequence, "+
			"using that reference sequence's own coordinates. "+
			"For coding features, the --labeledCodon option may be used. "+
			"In this case labeled codon locations <lcStart>, <lcEnd> are specified using the "+
			"codon-labeling scheme of the feature location. "+
			"The segment includes the reference sequence nucleotide at <refStart> (numbered from 1) "+
	"and subsequent nucleotides up to and including <refEnd>. "+
	"The new segment's endpoints must satisfy 1 <= refStart <= refEnd <= refSeqLength. "+
	"The new segment must not overlap any existing segment in the feature.") 
public class AddFeatureSegmentCommand extends FeatureLocModeCommand<CreateResult> {

	public static final String REF_START = "refStart";
	public static final String REF_END = "refEnd";
	public static final String LC_BASED = "labeledCodon";
	public static final String LC_START = "lcStart";
	public static final String LC_END = "lcEnd";
	public static final String TRANSLATION_MODIFIER = "translationModifier";
	public static final String SPLICE_INDEX = "spliceIndex";
	public static final String TRANSCRIPTION_INDEX = "transcriptionIndex";

	
	private Integer refStart;
	private Integer refEnd;
	private String lcStart;
	private String lcEnd;
	private Boolean labeledCodonBased;
	private String translationModifier;
	private Integer spliceIndex;
	private Integer transcriptionIndex;
	
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		refStart = PluginUtils.configureIntProperty(configElem, REF_START, false);
		refEnd = PluginUtils.configureIntProperty(configElem, REF_END, false);
		lcStart = PluginUtils.configureStringProperty(configElem, LC_START, false);
		lcEnd = PluginUtils.configureStringProperty(configElem, LC_END, false);
		labeledCodonBased = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, LC_BASED, false)).orElse(false);
		if(!( 
			(!labeledCodonBased && refStart != null && refEnd != null && lcStart == null && lcEnd == null) || 
			(labeledCodonBased && refStart == null && refEnd == null && lcStart != null && lcEnd != null) ) ) {
			throw new CommandException(CommandException.Code.COMMAND_USAGE_ERROR, "FeatureLocation segment must either be nucleotide or labeled-codon based.");
		}
		this.translationModifier = PluginUtils.configureStringProperty(configElem, TRANSLATION_MODIFIER, false);
		this.spliceIndex = PluginUtils.configureIntProperty(configElem, SPLICE_INDEX, 1);
		this.transcriptionIndex = PluginUtils.configureIntProperty(configElem, TRANSCRIPTION_INDEX, 1);

	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		if(labeledCodonBased) {
			Feature feature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(getFeatureName()));
			Feature parentFeature = feature.getParent();
			if(parentFeature == null) {
				throw new CommandException(CommandException.Code.COMMAND_FAILED_ERROR, "Feature \""+feature.getName()+"\" does not have a parent feature");
			}
			if(!parentFeature.codesAminoAcids()) {
				throw new CommandException(CommandException.Code.COMMAND_FAILED_ERROR, "Parent feature \""+parentFeature.getName()+"\" does not code for amino acids");
			}
			FeatureLocation parentFeatureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, 
					FeatureLocation.pkMap(getRefSeqName(), parentFeature.getName()));
			if(parentFeatureLoc == null) {
				throw new CommandException(CommandException.Code.COMMAND_FAILED_ERROR, "No feature location defined for parent feature \""+parentFeature.getName()+"\" on reference sequence \""+getRefSeqName()+"\"");
			}
			Map<String, LabeledCodon> labelToLabeledCodon = parentFeatureLoc.getLabelToLabeledCodon(cmdContext);
			LabeledCodon startLabeledCodon = labelToLabeledCodon.get(lcStart);
			if(startLabeledCodon == null) {
				throw new CommandException(CommandException.Code.COMMAND_FAILED_ERROR, "No such labeled codon \""+lcStart+"\"");
			}
			LabeledCodon endLabeledCodon = labelToLabeledCodon.get(lcEnd);
			if(endLabeledCodon == null) {
				throw new CommandException(CommandException.Code.COMMAND_FAILED_ERROR, "No such labeled codon \""+lcStart+"\"");
			}
			int minTranslationIndex = Math.min(startLabeledCodon.getTranslationIndex(), endLabeledCodon.getTranslationIndex());
			int maxTranslationIndex = Math.max(startLabeledCodon.getTranslationIndex(), endLabeledCodon.getTranslationIndex());
			List<ReferenceSegment> refSegs = new ArrayList<ReferenceSegment>();
			LabeledCodon[] transcriptionIndexToLabeledCodon = parentFeatureLoc.getTranslationIndexToLabeledCodon(cmdContext);
			for(int i = minTranslationIndex; i <= maxTranslationIndex; i++) {
				LabeledCodon lc = transcriptionIndexToLabeledCodon[i];
				List<LabeledCodonReferenceSegment> lcRefSegments = lc.getLcRefSegments();
				ReferenceSegment.sortByRefStart(lcRefSegments);
				List<LabeledCodonReferenceSegment> intersection = ReferenceSegment.intersection(refSegs, lcRefSegments, ReferenceSegment.cloneRightSegMerger());
				lcRefSegments = ReferenceSegment.subtract(lcRefSegments, intersection);
				refSegs.addAll(lcRefSegments);
			}
			refSegs = ReferenceSegment.mergeAbutting(refSegs, 
					ReferenceSegment.mergeAbuttingFunctionReferenceSegment(), ReferenceSegment.abutsPredicateReferenceSegment());
			if(refSegs.size() != 1) {
				throw new CommandException(CommandException.Code.COMMAND_FAILED_ERROR, "Region spanning start and end labeled codons "+refSegs.toString()+" is not a single nucleotide segment");
			}
			refStart = refSegs.get(0).getRefStart();
			refEnd = refSegs.get(0).getRefEnd();
		}
		TranslationModifier translationModifier = null;
		if(this.translationModifier != null) {
			Feature feature = GlueDataObject.lookup(cmdContext, Feature.class, Feature.pkMap(getFeatureName()));
			if(!feature.codesAminoAcids()) {
				throw new CommandException(CommandException.Code.COMMAND_FAILED_ERROR, "Feature \""+feature.getName()+"\" does not code for amino acids, so <translationModifier> cannot be used");
			}
			Module module = GlueDataObject.lookup(cmdContext, Module.class, Module.pkMap(this.translationModifier));
			ModulePlugin<?> modulePlugin = module.getModulePlugin(cmdContext);
			if(!(modulePlugin instanceof TranslationModifier)) {
				throw new CommandException(CommandException.Code.COMMAND_FAILED_ERROR, "Feature \""+feature.getName()+"\" does not code for amino acids, so <translationModifier> cannot be used");
			}
			translationModifier = (TranslationModifier) modulePlugin;
			int length = (refEnd - refStart) + 1;
			int segmentNtLength = translationModifier.getSegmentNtLength();
			if(length != segmentNtLength) {
				throw new CommandException(CommandException.Code.COMMAND_FAILED_ERROR, "FeatureSegment length of "+length+" does not match segmentNtLength of "+segmentNtLength+
						" on translationModifier module '"+translationModifier.getModuleName()+"'");
			}
		}

		
		FeatureLocation featureLoc = GlueDataObject.lookup(cmdContext, FeatureLocation.class, 
				FeatureLocation.pkMap(getRefSeqName(), getFeatureName()));
		if(refStart > refEnd) {
			throw new FeatureSegmentException(Code.FEATURE_SEGMENT_ENDPOINTS_REVERSED, 
					getRefSeqName(), getFeatureName(), Integer.toString(refStart), Integer.toString(refEnd));
		}
		Sequence refSequence = featureLoc.getReferenceSequence().getSequence();
		int refSeqLength = refSequence.getSequenceObject().getNucleotides(cmdContext).length();
		if(refStart < 1 || refEnd > refSeqLength) {
			throw new FeatureSegmentException(Code.FEATURE_SEGMENT_OUT_OF_RANGE, 
					getRefSeqName(), getFeatureName(), 
					Integer.toString(refSeqLength), Integer.toString(refStart), Integer.toString(refEnd));
		}
		List<ReferenceSegment> existingSegments = featureLoc.getSegments().stream()
				.map(FeatureSegment::asReferenceSegment)
				.collect(Collectors.toList());

		FeatureSegment featureSegment = GlueDataObject.create(cmdContext, FeatureSegment.class, 
				FeatureSegment.pkMap(getRefSeqName(), getFeatureName(), refStart, refEnd), false);
		
		List<ReferenceSegment> intersection = ReferenceSegment.intersection(existingSegments, 
				Collections.singletonList(featureSegment.asReferenceSegment()), 
				ReferenceSegment.cloneLeftSegMerger());
		
		if(!intersection.isEmpty()) {
			ReferenceSegment firstOverlap = intersection.get(0);
			throw new FeatureSegmentException(Code.FEATURE_SEGMENT_OVERLAPS_EXISTING, 
					getRefSeqName(), getFeatureName(), 
					Integer.toString(firstOverlap.getRefStart()), Integer.toString(firstOverlap.getRefEnd()));
		}
		
		featureSegment.setFeatureLocation(featureLoc);
		if(translationModifier != null) {
			featureSegment.setTranslationModifierName(this.translationModifier);
		}
		featureSegment.setSpliceIndex(this.spliceIndex);
		featureSegment.setTranscriptionIndex(this.transcriptionIndex);
		
		cmdContext.commit();
		return new CreateResult(FeatureSegment.class, 1);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("translationModifier", Module.class, Module.NAME_PROPERTY);
		}
	}
}
