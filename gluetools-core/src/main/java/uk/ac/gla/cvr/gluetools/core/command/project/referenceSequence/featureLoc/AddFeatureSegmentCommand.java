package uk.ac.gla.cvr.gluetools.core.command.project.referenceSequence.featureLoc;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.codonNumbering.LabeledCodon;
import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
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
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

@CommandClass( 
	commandWords={"add","segment"}, 
	docoptUsages={"( <refStart> <refEnd> | -l <lcStart> <lcEnd> )"},
	docoptOptions={
		"-l, --labeledCodon   Set location based on labeled codons"},
	metaTags={CmdMeta.updatesDatabase},
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

	
	private Integer refStart;
	private Integer refEnd;
	private String lcStart;
	private String lcEnd;
	private Boolean labeledCodonBased;
	
	
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
			refStart = startLabeledCodon.getNtStart();
			LabeledCodon endLabeledCodon = labelToLabeledCodon.get(lcEnd);
			if(endLabeledCodon == null) {
				throw new CommandException(CommandException.Code.COMMAND_FAILED_ERROR, "No such labeled codon \""+lcStart+"\"");
			}
			refEnd = endLabeledCodon.getNtStart()+2;
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
		cmdContext.commit();
		return new CreateResult(FeatureSegment.class, 1);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
		}
	}
}
