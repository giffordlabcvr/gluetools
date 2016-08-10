package uk.ac.gla.cvr.gluetools.core.datamodel.customtable;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._CustomTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.customtableobject.CustomTableObject;

@GlueDataClass(defaultListedProperties = {_CustomTable.NAME_PROPERTY})
public class CustomTable extends _CustomTable {

	private Class<? extends CustomTableObject> rowClass;
	
	public Class<? extends CustomTableObject> getRowClass() {
		return rowClass;
	}

	public void setRowObjectClass(Class<? extends CustomTableObject> rowClass) {
		this.rowClass = rowClass;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setName(pkMap.get(NAME_PROPERTY));
	}

	@Override
	public Map<String, String> pkMap() {
		return pkMap(getProject().getName(), getName());
	}

	public static Map<String, String> pkMap(String projectName, String name) {
		Map<String, String> pkMap = new LinkedHashMap<String,String>();
		pkMap.put(PROJECT_PROPERTY+"."+_Project.NAME_PROPERTY, projectName);
		pkMap.put(NAME_PROPERTY, name);
		return pkMap;
	}
	
}
