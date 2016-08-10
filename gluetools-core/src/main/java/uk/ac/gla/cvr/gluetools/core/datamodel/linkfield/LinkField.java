package uk.ac.gla.cvr.gluetools.core.datamodel.linkfield;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._LinkField;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Project;

@GlueDataClass(defaultListedProperties = {_LinkField.TABLE_PROPERTY, _LinkField.NAME_PROPERTY, _LinkField.DEST_TABLE_NAME_PROPERTY})
public class LinkField extends _LinkField {

	public static Map<String, String> pkMap(String projectName, String table, String name) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(PROJECT_PROPERTY+"."+_Project.NAME_PROPERTY, projectName);
		idMap.put(TABLE_PROPERTY, table);
		idMap.put(NAME_PROPERTY, name);
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setName(pkMap.get(NAME_PROPERTY));
		setTable(pkMap.get(TABLE_PROPERTY));
	}

	@Override
	public Map<String, String> pkMap() {
		return pkMap(getProject().getName(), getTable(), getName());
	}

}
