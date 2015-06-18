package uk.ac.gla.cvr.gluetools.core.datamodel;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Sequence;

@GlueDataClass(listColumnHeaders = {_Sequence.SOURCE_PROPERTY, _Sequence.SOURCE_ID_PROPERTY})
public class Sequence extends _Sequence {

	@Override
	public String[] populateListRow() {
		return new String[]{getSource().getName(), getSourceId()};
	}

	public static Map<String, String> pkMap(String sourceName, String sourceId) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(SOURCE_PK_COLUMN, sourceName);
		idMap.put(SOURCE_ID_PK_COLUMN, sourceId);
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setSourceId(pkMap.get(SOURCE_ID_PK_COLUMN));
	}
}