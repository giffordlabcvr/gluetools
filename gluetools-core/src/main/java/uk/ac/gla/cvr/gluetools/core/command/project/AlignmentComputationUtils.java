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
package uk.ac.gla.cvr.gluetools.core.command.project;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.cayenne.exp.Expression;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.QueryMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext.ModeCloser;
import uk.ac.gla.cvr.gluetools.core.command.project.ExtendAlignmentCommand.ExtendAlignmentResult;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.MemberAddSegmentCommand;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.member.MemberRemoveSegmentCommand;
import uk.ac.gla.cvr.gluetools.core.command.result.CreateResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.segments.IQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

public class AlignmentComputationUtils {

	public static Map<String, String> getMembersNtMap(CommandContext cmdContext, List<Map<String, String>> memberPkMaps) {
		Map<String, String> queryIdToNucleotides = new LinkedHashMap<String, String>();
		for(Map<String, String> memberPkMap: memberPkMaps) {
			AlignmentMember almtMember = GlueDataObject.lookup(cmdContext, AlignmentMember.class, memberPkMap);
			Sequence sequence = almtMember.getSequence();
			String nucleotides = sequence.getSequenceObject().getNucleotides(cmdContext);
			String queryId = constructQueryId(sequence.getSource().getName(), sequence.getSequenceID());
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

	public static List<Map<String, String>> getMemberPkMaps(CommandContext cmdContext, String alignmentName, Expression whereClause) {
		List<Map<String, String>> memberPkMaps = new ArrayList<Map<String, String>>();
		
		QueryMemberSupplier memberSupplier = new QueryMemberSupplier(alignmentName, false, Optional.of(whereClause));
		
		int numMembers = memberSupplier.countMembers(cmdContext);
		int offset = 0;
		int batchSize = 500;
		while(offset < numMembers) {
			List<AlignmentMember> almtMembers = memberSupplier.supplyMembers(cmdContext, offset, batchSize);
			almtMembers.forEach(memb -> memberPkMaps.add(memb.pkMap()));
			offset += batchSize;
			cmdContext.newObjectContext();
		}
		return memberPkMaps;
	}


	

	
}
