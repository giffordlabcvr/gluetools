package uk.ac.gla.cvr.gluetools.core.groupMember;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._GroupMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._SequenceGroup;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Source;

@GlueDataClass(defaultListColumns = {GroupMember.SOURCE_NAME_PATH, GroupMember.SEQUENCE_ID_PATH})
public class GroupMember extends _GroupMember {

	public static final String GROUP_NAME_PATH = 
			_GroupMember.GROUP_PROPERTY+"."+_SequenceGroup.NAME_PROPERTY;

	public static final String SOURCE_NAME_PATH = 
			_GroupMember.SEQUENCE_PROPERTY+"."+
					_Sequence.SOURCE_PROPERTY+"."+_Source.NAME_PROPERTY;
	
	public static final String SEQUENCE_ID_PATH = 
			_GroupMember.SEQUENCE_PROPERTY+"."+_Sequence.SEQUENCE_ID_PROPERTY;
	
	public static Map<String, String> pkMap(String groupName,
			String sourceName, String sequenceID) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(GROUP_NAME_PATH, groupName);
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
				getGroup().getName(),
				getSequence().getSource().getName(), 
				getSequence().getSequenceID());
	}
	

}
