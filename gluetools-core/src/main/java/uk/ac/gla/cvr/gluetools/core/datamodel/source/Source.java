package uk.ac.gla.cvr.gluetools.core.datamodel.source;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Source;

// TODO have a default source which always exists. Command arguments which require a source-name default to this.
@GlueDataClass(listColumnHeaders = {_Source.NAME_PROPERTY})
public class Source extends _Source {

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