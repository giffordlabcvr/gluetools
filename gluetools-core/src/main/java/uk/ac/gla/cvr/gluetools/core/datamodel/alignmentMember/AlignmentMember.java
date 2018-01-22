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
package uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignedSegment.AlignedSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMemberException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Source;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.segments.IQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

@GlueDataClass(
		defaultObjectRendererFtlFile = "defaultRenderers/alignmentMember.ftlx",
		defaultListedProperties = {AlignmentMember.ALIGNMENT_NAME_PATH, AlignmentMember.SOURCE_NAME_PATH, AlignmentMember.SEQUENCE_ID_PATH},
		listableBuiltInProperties = {AlignmentMember.ALIGNMENT_NAME_PATH, AlignmentMember.SOURCE_NAME_PATH, AlignmentMember.SEQUENCE_ID_PATH, AlignmentMember.REFERENCE_MEMBER_PROPERTY}
	)
public class AlignmentMember extends _AlignmentMember {
	
	public enum MemberStatistic {
		referenceNtCoveragePercent,
		memberNtCoveragePercent
	}

	public static final String ALIGNMENT_NAME_PATH = 
			_AlignmentMember.ALIGNMENT_PROPERTY+"."+_Alignment.NAME_PROPERTY;

	public static final String SOURCE_NAME_PATH = 
			_AlignmentMember.SEQUENCE_PROPERTY+"."+
					_Sequence.SOURCE_PROPERTY+"."+_Source.NAME_PROPERTY;
	
	public static final String SEQUENCE_ID_PATH = 
			_AlignmentMember.SEQUENCE_PROPERTY+"."+_Sequence.SEQUENCE_ID_PROPERTY;
	
	public static Map<String, String> pkMap(String alignmentName,
			String sourceName, String sequenceID) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(ALIGNMENT_NAME_PATH, alignmentName);
		idMap.put(SOURCE_NAME_PATH, sourceName);
		idMap.put(SEQUENCE_ID_PATH, sequenceID);
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
	}
	
	@Override
	public Map<String, String> pkMap() {
		return pkMap(
				getAlignment().getName(),
				getSequence().getSource().getName(), 
				getSequence().getSequenceID());
	}
	
	
	public Map<String, Object> getStatistics(List<AlignmentMember.MemberStatistic> statistics, CommandContext cmdContext) {
		Map<String, Object> results = new LinkedHashMap<String, Object>();
		if(statistics.contains(AlignmentMember.MemberStatistic.referenceNtCoveragePercent)) {
			results.put(AlignmentMember.MemberStatistic.referenceNtCoveragePercent.name(), getReferenceNtCoveragePercent(cmdContext));
		}
		if(statistics.contains(AlignmentMember.MemberStatistic.memberNtCoveragePercent)) {
			results.put(AlignmentMember.MemberStatistic.memberNtCoveragePercent.name(), getMemberNtCoveragePercent(cmdContext));
		}

		return results;
	}

	public Double getMemberNtCoveragePercent(CommandContext cmdContext) {
		int memberLength = getSequence().getSequenceObject().getNucleotides(cmdContext).length();
		List<AlignedSegment> alignedSegments = getAlignedSegments();
		return IQueryAlignedSegment.getQueryNtCoveragePercent(alignedSegments, memberLength);
	}

	public Double getReferenceNtCoveragePercent(CommandContext cmdContext) {
		ReferenceSequence refSequence = getAlignment().getRefSequence();
		if(refSequence == null) {
			return null;
		}
		int referenceLength = refSequence.
				getSequence().getSequenceObject().getNucleotides(cmdContext).length();
		List<AlignedSegment> alignedSegments = getAlignedSegments();
		return IQueryAlignedSegment.getReferenceNtCoveragePercent(alignedSegments, referenceLength);
	}
	
	public List<QueryAlignedSegment> segmentsAsQueryAlignedSegments() {
		return getAlignedSegments().stream().map(seg -> seg.asQueryAlignedSegment()).collect(Collectors.toList());
	}
	
	public ReferenceSequence targetReferenceFromMember() {
		List<ReferenceSequence> memberReferences = this.getSequence().getReferenceSequences();
		Alignment memberAlmt = this.getAlignment();
		ReferenceSequence targetRef = null;
		if(memberReferences.isEmpty()) {
			targetRef = memberAlmt.getConstrainingRef();
		} else if(memberReferences.size() == 1) {
			// single reference, choose that.
			targetRef = memberReferences.get(0);
		} else {
			// if one isn't a constraining ref of any alignment, choose that.
			for(ReferenceSequence refSeq: memberReferences) {
				if(refSeq.getAlignmentsWhereRefSequence().isEmpty()) {
					targetRef = refSeq;
					break;
				}
			}
			// otherwise if one of the references is the constraining ref of the tip alignment, choose that.
			if(targetRef == null) {
				for(ReferenceSequence refSeq: memberReferences) {
					if(refSeq.getName().equals(memberAlmt.getConstrainingRef().getName())) {
						targetRef = refSeq;
						break;
					}
				}
			}
			// otherwise sort by REF name and choose first.
			if(targetRef == null) {
				for(ReferenceSequence refSeq: memberReferences) {
					if(targetRef == null || refSeq.getName().compareTo(targetRef.getName()) < 0) {
						targetRef = refSeq;
					}
				}
			}
			if(targetRef == null) {
				throw new AlignmentMemberException(Code.CANNOT_DETERMINE_TARGET_REFERENCE_FROM_ALIGNMENT_MEMBER, 
						memberAlmt.getName(), this.getSequence().getSource().getName(), this.getSequence().getSequenceID());
			}
		}
		return targetRef;
	}
	
}
