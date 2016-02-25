package uk.ac.gla.cvr.gluetools.core.datamodel.field;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Field;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Project;

// TODO create field should have a default value option.
@GlueDataClass(defaultListedFields = {_Field.TABLE_PROPERTY, _Field.NAME_PROPERTY, _Field.TYPE_PROPERTY, _Field.MAX_LENGTH_PROPERTY})
public class Field extends _Field {

	private FieldType fieldType = null;
	
	public FieldType getFieldType() {
		if(fieldType == null) {
			fieldType = FieldType.valueOf(getType());
		}
		return fieldType;
	}
	
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
