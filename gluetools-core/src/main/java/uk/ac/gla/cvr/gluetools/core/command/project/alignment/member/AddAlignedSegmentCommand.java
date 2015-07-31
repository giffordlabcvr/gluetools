package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import org.apache.cayenne.ObjectContext;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.AlignedSegmentException.Code;
import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignedSegment.AlignedSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

// TODO add the constraint that, in a given alignment member, each reference sequence nucleotide may only be aligned with one
// member sequence nucleotide.
@CommandClass( 
	commandWords={"add","segment"}, 
	docoptUsages={"<refStart> <refEnd> <memberStart> <memberEnd>"},
	description="Add a new aligned segment", 
	furtherHelp=
	"An aligned segment is a proposed homology between a contiguous region of the reference sequence "+
	"and a contiguous region of the member sequence, where the two regions are of equal size. "+
	"In both cases the region includes the nucleotide at the start point (numbered from 1) "+
	"and subsequent nucleotides up to and including the end point. "+
	"The reference region endpoints must satisfy 1 <= refStart < refEnd <= refSeqLength. "+
	"Similarly, the member region endpoints must be within membSeqLength. "+
	"It is permissible for memberStart > memberEnd. This indicates a homology in the reverse direction. "+
	"In this case the member region includes the nucleotide at memberEnd and the nucleotide at memberStart.") 
public class AddAlignedSegmentCommand extends MemberModeCommand {

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
	public CommandResult execute(CommandContext cmdContext) {
		ObjectContext objContext = cmdContext.getObjectContext();
		if(refStart > refEnd) {
			throw new AlignedSegmentException(Code.ALIGNED_SEGMENT_REF_REGION_ENDPOINTS_REVERSED, 
					getAlignmentName(), getSourceName(), getSequenceID(), Integer.toString(refStart), Integer.toString(refEnd));
		}
		AlignmentMember almtMemb = GlueDataObject.lookup(cmdContext.getObjectContext(), AlignmentMember.class, 
				AlignmentMember.pkMap(getAlignmentName(), getSourceName(), getSequenceID()));
		Sequence refSequence = almtMemb.getAlignment().getRefSequence().getSequence();
		int refSeqLength = refSequence.getNucleotides().length();
		if(refStart < 1 || refEnd > refSeqLength) {
			throw new AlignedSegmentException(Code.ALIGNED_SEGMENT_REF_REGION_OUT_OF_RANGE, 
					getAlignmentName(), getSourceName(), getSequenceID(), 
					Integer.toString(refSeqLength), Integer.toString(refStart), Integer.toString(refEnd));
		}
		Sequence membSequence = almtMemb.getSequence();
		int membSeqLength = membSequence.getNucleotides().length();
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
		// TODO specify and enforce further constraints as necessary. 
		AlignedSegment alignedSegment = GlueDataObject.create(objContext, AlignedSegment.class, 
				AlignedSegment.pkMap(getAlignmentName(), getSourceName(), getSequenceID(), refStart, refEnd, memberStart, memberEnd), false);
		alignedSegment.setAlignmentMember(almtMemb);
		cmdContext.commit();
		return new CreateResult(AlignedSegment.class, 1);
	}

}
