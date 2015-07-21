package uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Source;

@GlueDataClass(defaultListColumns = {AlignmentMember.SOURCE_NAME_PATH, AlignmentMember.SEQUENCE_ID_PATH})
public class AlignmentMember extends _AlignmentMember {
	
	public static final String SOURCE_NAME_PATH = 
			_AlignmentMember.SEQUENCE_PROPERTY+"."+
					_Sequence.SOURCE_PROPERTY+"."+_Source.NAME_PROPERTY;
	
	public static final String SEQUENCE_ID_PATH = 
			_AlignmentMember.SEQUENCE_PROPERTY+"."+_Sequence.SEQUENCE_ID_PROPERTY;

	public static final String ALIGNMENT_NAME_PATH = 
			_AlignmentMember.ALIGNMENT_PROPERTY+"."+_Alignment.NAME_PROPERTY;
	
	
	public static Map<String, String> pkMap(
			String sourceName, String sequenceID, String alignmentName) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(SOURCE_NAME_PATH, sourceName);
		idMap.put(SEQUENCE_ID_PATH, sequenceID);
		idMap.put(ALIGNMENT_NAME_PATH, alignmentName);
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
	}
	
	@Override
	public Map<String, String> pkMap() {
		return pkMap(
				getSequence().getSource().getName(), 
				getSequence().getSequenceID(),
				getAlignment().getName());
	}

}
