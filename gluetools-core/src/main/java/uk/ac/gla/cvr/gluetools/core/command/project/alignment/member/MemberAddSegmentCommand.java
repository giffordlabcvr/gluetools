package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.AlignedSegmentException.Code;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignedSegment.AlignedSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;

// TODO add the constraint that, in a given alignment member, each reference sequence nucleotide may only be aligned with one
// member sequence nucleotide.
@CommandClass( 
	commandWords={"add","segment"}, 
	docoptUsages={"<refStart> <refEnd> <memberStart> <memberEnd>"},
	description="Add a new aligned segment", 
	metaTags={CmdMeta.updatesDatabase},
	furtherHelp=
	"An aligned segment is a proposed homology between a contiguous region of the reference sequence "+
	"and a contiguous region of the member sequence, where the two regions are of equal size. "+
	"In both cases the region includes the nucleotide at the start point (numbered from 1) "+
	"and subsequent nucleotides up to and including the end point. "+
	"The reference region endpoints must satisfy 1 <= refStart < refEnd <= refSeqLength. "+
	"Similarly, the member region endpoints must be within membSeqLength. "+
	"It is permissible for memberStart > memberEnd. This indicates a homology in the reverse direction. "+
	"In this case the member region includes the nucleotide at memberEnd and the nucleotide at memberStart.") 
public class MemberAddSegmentCommand extends MemberModeCommand<CreateResult> {

	public static final String REF_START = "refStart";
	public static final String REF_END = "refEnd";
	public static final String MEMBER_START = "memberStart";
	public static final String MEMBER_END = "memberEnd";
	
	private int refStart;
	private int refEnd;
	private int memberStart;
	private int memberEnd;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		refStart = PluginUtils.configureIntProperty(configElem, REF_START, true);
		refEnd = PluginUtils.configureIntProperty(configElem, REF_END, true);
		memberStart = PluginUtils.configureIntProperty(configElem, MEMBER_START, true);
		memberEnd = PluginUtils.configureIntProperty(configElem, MEMBER_END, true);
	}

	@Override
	public CreateResult execute(CommandContext cmdContext) {
		
		if(refStart > refEnd) {
			throw new AlignedSegmentException(Code.ALIGNED_SEGMENT_REF_REGION_ENDPOINTS_REVERSED, 
					getAlignmentName(), getSourceName(), getSequenceID(), Integer.toString(refStart), Integer.toString(refEnd));
		}
		AlignmentMember almtMemb = GlueDataObject.lookup(cmdContext, AlignmentMember.class, 
				AlignmentMember.pkMap(getAlignmentName(), getSourceName(), getSequenceID()));
		Alignment alignment = almtMemb.getAlignment();
		ReferenceSequence refSeq = alignment.getRefSequence();
		if(refSeq != null) {
			Sequence refSeqSequence = refSeq.getSequence();
			int refSeqLength = refSeqSequence.getSequenceObject().getNucleotides(cmdContext).length();
			if(refStart < 1 || refEnd > refSeqLength) {
				throw new AlignedSegmentException(Code.ALIGNED_SEGMENT_REF_REGION_OUT_OF_RANGE, 
						getAlignmentName(), getSourceName(), getSequenceID(), 
						Integer.toString(refSeqLength), Integer.toString(refStart), Integer.toString(refEnd));
			}
		}
		Sequence membSequence = almtMemb.getSequence();
		int membSeqLength = membSequence.getSequenceObject().getNucleotides(cmdContext).length();
		if(memberStart < 1 || memberEnd > membSeqLength || 
				memberEnd < 1 || memberStart > membSeqLength) {
			throw new AlignedSegmentException(Code.ALIGNED_SEGMENT_MEMBER_REGION_OUT_OF_RANGE, 
					getAlignmentName(), getSourceName(), getSequenceID(), 
					Integer.toString(membSeqLength), Integer.toString(memberStart), Integer.toString(memberEnd));
		}
		int refRegionLength = (refEnd - refStart)+1;
		int membRegionLength = Math.abs(memberEnd - memberStart)+1;
		if(membRegionLength != refRegionLength) {
			throw new AlignedSegmentException(Code.ALIGNED_SEGMENT_REGION_LENGTHS_NOT_EQUAL, 
					getAlignmentName(), getSourceName(), getSequenceID(), 
					Integer.toString(membRegionLength), Integer.toString(refRegionLength));
		}
		List<QueryAlignedSegment> existingSegments = almtMemb
				.getAlignedSegments().stream().map(AlignedSegment::asQueryAlignedSegment).collect(Collectors.toList());
		
		// TODO specify and enforce further constraints as necessary. 
		AlignedSegment alignedSegment = GlueDataObject.create(cmdContext, AlignedSegment.class, 
				AlignedSegment.pkMap(getAlignmentName(), getSourceName(), getSequenceID(), refStart, refEnd, memberStart, memberEnd), false);

		List<QueryAlignedSegment> intersection = ReferenceSegment.intersection(existingSegments, 
				Collections.singletonList(alignedSegment.asQueryAlignedSegment()), 
				ReferenceSegment.cloneLeftSegMerger());
		
		if(!intersection.isEmpty()) {
			QueryAlignedSegment firstOverlap = intersection.get(0);
			throw new AlignedSegmentException(Code.ALIGNED_SEGMENT_OVERLAPS_EXISTING, 
					getAlignmentName(), getSourceName(), getSequenceID(), 
					Integer.toString(firstOverlap.getRefStart()), Integer.toString(firstOverlap.getRefEnd()));
		}
		
		alignedSegment.setAlignmentMember(almtMemb);
		cmdContext.commit();
		return new CreateResult(AlignedSegment.class, 1);
	}

	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
		}
	}
}
