package uk.ac.gla.cvr.gluetools.core.datamodel.field;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datafield.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Field;

// TODO projects should have a data-field table linked to sequences.
// TODO creation / deletion of fields should modify the relevant table in the DB.
@GlueDataClass(listColumnHeaders = {_Field.NAME_PROPERTY, _Field.TYPE_PROPERTY})
public class Field extends _Field {

	private FieldType fieldType = null;
	
	@Override
	public String[] populateListRow() {
		return new String[]{
				getName(), 
				getType()};
	}

	public FieldType getFieldType() {
		if(fieldType == null) {
			fieldType = FieldType.valueOf(getType());
		}
		return fieldType;
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
