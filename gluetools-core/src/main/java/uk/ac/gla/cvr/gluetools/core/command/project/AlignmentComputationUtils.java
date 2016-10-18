package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.exp.Expression;

import uk.ac.gla.cvr.gluetools.core.command.CommandBuilder;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.project.ExtendAlignmentCommand.ExtendAlignmentResult;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.MemberAddSegmentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.MemberRemoveSegmentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.OriginalDataResult;
import uk.ac.gla.cvr.gluetools.core.command.project.sequence.ShowOriginalDataCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.command.result.ListResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.AbstractSequenceObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceFormat;
import uk.ac.gla.cvr.gluetools.core.segments.IQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

public class AlignmentComputationUtils {

	public static Map<String, String> getMembersNtMap(CommandContext cmdContext, List<Map<String, Object>> memberIDs) {
		Map<String, String> queryIdToNucleotides = new LinkedHashMap<String, String>();
		for(Map<String, Object> memberIDmap: memberIDs) {
			String memberSourceName = (String) memberIDmap.get(AlignmentMember.SOURCE_NAME_PATH);
			String memberSeqId = (String) memberIDmap.get(AlignmentMember.SEQUENCE_ID_PATH);
			OriginalDataResult memberSeqOriginalData = getOriginalData(cmdContext, memberSourceName, memberSeqId);
			SequenceFormat memberSeqFormat = memberSeqOriginalData.getFormat();
			byte[] base64Bytes = memberSeqOriginalData.getBase64Bytes();
			AbstractSequenceObject memberSeqObject = memberSeqFormat.sequenceObject();
			memberSeqObject.fromOriginalData(base64Bytes);
			String nucleotides = memberSeqObject.getNucleotides(cmdContext);
			String queryId = constructQueryId(memberSourceName, memberSeqId);
			queryIdToNucleotides.put(queryId, nucleotides);
		}
		return queryIdToNucleotides;
	}

	public static String constructQueryId(String sourceName, String sequenceID) {
		return sourceName+"."+sequenceID;
	}

	public static Map<String, Object> applyMemberAlignedSegments(
			CommandContext cmdContext,
			Map<String, String> memberPkMap,
			List<QueryAlignedSegment> memberAlignedSegments) {
		return applyMemberAlignedSegments(cmdContext, 
				memberPkMap.get(AlignmentMember.ALIGNMENT_NAME_PATH), 
				memberPkMap.get(AlignmentMember.SOURCE_NAME_PATH), 
				memberPkMap.get(AlignmentMember.SEQUENCE_ID_PATH), 
				memberAlignedSegments);
	}
	
	public static Map<String, Object> applyMemberAlignedSegments(
			CommandContext cmdContext,
			String alignmentName,
			String memberSourceName,
			String memberSeqId,
			List<QueryAlignedSegment> memberAlignedSegments) {
		// enter the relevant alignment member mode, delete the existing aligned segments, and add new segments
		// according to the aligner result.
		int numRemovedSegments = 0;
		int numAddedSegments = 0;
		if(memberAlignedSegments != null) {
			try(ModeCloser almtMode = cmdContext.pushCommandMode("alignment", alignmentName)) {
				try(ModeCloser memberMode = cmdContext.pushCommandMode("member", memberSourceName, memberSeqId)) {
					numRemovedSegments = cmdContext.cmdBuilder(MemberRemoveSegmentCommand.class)
							.set(MemberRemoveSegmentCommand.ALL_SEGMENTS, true)
							.execute().getNumber();
					for(IQueryAlignedSegment alignedSegment: memberAlignedSegments) {
						CreateResult addSegResult = cmdContext.cmdBuilder(MemberAddSegmentCommand.class)
								.set(MemberAddSegmentCommand.REF_START, alignedSegment.getRefStart())
								.set(MemberAddSegmentCommand.REF_END, alignedSegment.getRefEnd())
								.set(MemberAddSegmentCommand.MEMBER_START, alignedSegment.getQueryStart())
								.set(MemberAddSegmentCommand.MEMBER_END, alignedSegment.getQueryEnd())
								.execute();
						numAddedSegments = numAddedSegments + addSegResult.getNumber();
					}
				}
			}
		}
		return createMemberResultMap(memberSourceName, memberSeqId,
				numRemovedSegments, numAddedSegments);
	}


	public static Map<String, Object> createMemberResultMap(String memberSourceName,
			String memberSeqId, int numRemovedSegments, int numAddedSegments) {
		Map<String, Object> memberResultMap = new LinkedHashMap<String, Object>();
		memberResultMap.put(AlignmentMember.SOURCE_NAME_PATH, memberSourceName);
		memberResultMap.put(AlignmentMember.SEQUENCE_ID_PATH, memberSeqId);
		memberResultMap.put(ExtendAlignmentResult.REMOVED_SEGMENTS, numRemovedSegments);
		memberResultMap.put(ExtendAlignmentResult.ADDED_SEGMENTS, numAddedSegments);
		return memberResultMap;
	}

	public static OriginalDataResult getOriginalData(CommandContext cmdContext, String sourceName, String seqId) {
		// enter the sequence command mode to get the sequence original data.
		try (ModeCloser refSeqMode = cmdContext.pushCommandMode("sequence", sourceName, seqId)) {
			return cmdContext.cmdBuilder(ShowOriginalDataCommand.class).execute();
		}
	}

	public static List<Map<String, Object>> getMemberSequenceIdMaps(CommandContext cmdContext, String alignmentName, Expression whereClause) {
		try (ModeCloser refMode = cmdContext.pushCommandMode("alignment", alignmentName)) {
			CommandBuilder<ListResult, AlignmentListMemberCommand> cmdBuilder = cmdContext.cmdBuilder(AlignmentListMemberCommand.class);
			if(whereClause != null) {
				cmdBuilder.set(AbstractListCTableCommand.WHERE_CLAUSE, whereClause.toString());
			}
			cmdBuilder.setArray(AbstractListCTableCommand.FIELD_NAME)
				.add(AlignmentMember.SOURCE_NAME_PATH)
				.add(AlignmentMember.SEQUENCE_ID_PATH);
			return cmdBuilder.execute().asListOfMaps();
		}
	}


	

	
}
