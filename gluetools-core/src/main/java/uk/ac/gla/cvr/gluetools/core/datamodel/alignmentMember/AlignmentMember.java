package uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignedSegment.AlignedSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Source;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.segments.IQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;

@GlueDataClass(defaultListColumns = {AlignmentMember.ALIGNMENT_NAME_PATH, AlignmentMember.SOURCE_NAME_PATH, AlignmentMember.SEQUENCE_ID_PATH})
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
	
}
