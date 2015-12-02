package uk.ac.gla.cvr.gluetools.core.target;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Source;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Target;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._TargetSet;

@GlueDataClass(defaultListColumns = {Target.SOURCE_NAME_PATH, Target.SEQUENCE_ID_PATH})
public class Target extends _Target {

	public static final String TARGET_NAME_PATH = 
			_Target.TARGET_SET_PROPERTY+"."+_TargetSet.NAME_PROPERTY;

	public static final String SOURCE_NAME_PATH = 
			_Target.SEQUENCE_PROPERTY+"."+
					_Sequence.SOURCE_PROPERTY+"."+_Source.NAME_PROPERTY;
	
	public static final String SEQUENCE_ID_PATH = 
			_Target.SEQUENCE_PROPERTY+"."+_Sequence.SEQUENCE_ID_PROPERTY;
	
	public static Map<String, String> pkMap(String targetName,
			String sourceName, String sequenceID) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(TARGET_NAME_PATH, targetName);
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
				getTargetSet().getName(),
				getSequence().getSource().getName(), 
				getSequence().getSequenceID());
	}
	

}
