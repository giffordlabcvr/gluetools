package uk.ac.gla.cvr.gluetools.core.datamodel.alignedSegment;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._AlignedSegment;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Source;

@GlueDataClass(defaultListColumns = {
		_AlignedSegment.REF_START_PROPERTY, 
		_AlignedSegment.REF_END_PROPERTY, 
		_AlignedSegment.MEMBER_START_PROPERTY, 
		_AlignedSegment.MEMBER_END_PROPERTY})
public class AlignedSegment extends _AlignedSegment {

	public static final String MEMBER_SOURCE_NAME_PATH = 
			_AlignedSegment.ALIGNMENT_MEMBER_PROPERTY+"."+_AlignmentMember.SEQUENCE_PROPERTY+"."+
					_Sequence.SOURCE_PROPERTY+"."+_Source.NAME_PROPERTY;
	
	public static final String MEMBER_SEQUENCE_ID_PATH = 
			_AlignedSegment.ALIGNMENT_MEMBER_PROPERTY+"."+_AlignmentMember.SEQUENCE_PROPERTY+"."+
					_Sequence.SEQUENCE_ID_PROPERTY;

	public static final String ALIGNMENT_NAME_PATH = 
			_AlignedSegment.ALIGNMENT_MEMBER_PROPERTY+"."+_AlignmentMember.ALIGNMENT_PROPERTY+"."+
					_Alignment.NAME_PROPERTY;
	
	
	public static Map<String, String> pkMap(String alignmentName, 
			String memberSourceName, String memberSequenceID,
			int refStart, int refEnd, int memberStart, int memberEnd) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(MEMBER_SOURCE_NAME_PATH, memberSourceName);
		idMap.put(MEMBER_SEQUENCE_ID_PATH, memberSequenceID);
		idMap.put(ALIGNMENT_NAME_PATH, alignmentName);
		idMap.put(REF_START_PROPERTY, Integer.toString(refStart));
		idMap.put(REF_END_PROPERTY, Integer.toString(refEnd));
		idMap.put(MEMBER_START_PROPERTY, Integer.toString(memberStart));
		idMap.put(MEMBER_END_PROPERTY, Integer.toString(memberEnd));
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setRefStart(Integer.parseInt(pkMap.get(REF_START_PROPERTY)));
		setRefEnd(Integer.parseInt(pkMap.get(REF_END_PROPERTY)));
		setMemberStart(Integer.parseInt(pkMap.get(MEMBER_START_PROPERTY)));
		setMemberEnd(Integer.parseInt(pkMap.get(MEMBER_END_PROPERTY)));
	}
	
	@Override
	protected Map<String, String> pkMap() {
		return pkMap(
				getAlignmentMember().getSequence().getSource().getName(), 
				getAlignmentMember().getSequence().getSequenceID(),
				getAlignmentMember().getAlignment().getName(),
				getRefStart(), 
				getRefEnd(), 
				getMemberStart(), 
				getMemberEnd());
	}
}