package uk.ac.gla.cvr.gluetools.core.datamodel.seqOrigData;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.auto._SeqOrigData;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Source;

public class SeqOrigData extends _SeqOrigData {

	public static final String SOURCE_NAME_PATH = _SeqOrigData.SEQUENCE_PROPERTY+"."+_Sequence.SOURCE_PROPERTY+"."+_Source.NAME_PROPERTY;
	public static final String SEQUENCE_ID_PATH = _SeqOrigData.SEQUENCE_PROPERTY+"."+_Sequence.SEQUENCE_ID_PROPERTY;

	
	public static Map<String, String> pkMap(String sourceName, String sequenceID) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(SOURCE_NAME_PATH, sourceName);
		idMap.put(SEQUENCE_ID_PATH, sequenceID);
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
	}
	
	@Override
	public Map<String, String> pkMap() {
		return pkMap(getSequence().getSource().getName(), getSequence().getSequenceID());
	}

}
