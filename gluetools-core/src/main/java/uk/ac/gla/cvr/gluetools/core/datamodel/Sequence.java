package uk.ac.gla.cvr.gluetools.core.datamodel;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Sequence;

@GlueDataClass(listColumnHeaders = {_Sequence.SOURCE_PROPERTY, _Sequence.SEQUENCE_ID_PROPERTY, _Sequence.FORMAT_PROPERTY})
public class Sequence extends _Sequence {

	@Override
	public String[] populateListRow() {
		return new String[]{getSource().getName(), getSequenceID(), getFormat()};
	}

	public static Map<String, String> pkMap(String sourceName, String sourceId) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(SOURCE_PK_COLUMN, sourceName);
		idMap.put(SEQUENCE_ID_PK_COLUMN, sourceId);
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setSequenceID(pkMap.get(SEQUENCE_ID_PK_COLUMN));
	}
}