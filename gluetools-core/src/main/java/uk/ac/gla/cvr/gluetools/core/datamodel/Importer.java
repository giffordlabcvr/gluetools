package uk.ac.gla.cvr.gluetools.core.datamodel;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Importer;

@GlueDataClass(listColumnHeaders = {_Importer.NAME_PROPERTY})
public class Importer extends _Importer {

	@Override
	public String[] populateListRow() {
		return new String[]{getName()};
	}

	public static Map<String, String> pkMap(String projectName, String name) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(PROJECT_PK_COLUMN, projectName);
		idMap.put(NAME_PK_COLUMN, name);
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setName(pkMap.get(NAME_PK_COLUMN));
	}
	
	
}