package uk.ac.gla.cvr.gluetools.core.datamodel;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;

import uk.ac.gla.cvr.gluetools.core.datamodel.DataModelException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Field;

@GlueDataClass(listColumnHeaders = {"ColumnName", "Type"})
public class Field extends _Field {

	@Override
	public String[] populateListRow() {
		return new String[]{
				getObjectId().getIdSnapshot().get(COLUMN_NAME_PK_COLUMN).toString(), 
				getType()};
	}

	public static Field lookupField(ObjectContext objContext, String projectId, String columnName) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(PROJECT_PK_COLUMN, projectId);
		idMap.put(COLUMN_NAME_PK_COLUMN, columnName);
		Field field = Cayenne.objectForPK(objContext, Field.class, idMap);
		if(field == null) {
			throw new DataModelException(Code.OBJECT_NOT_FOUND, Field.class.getSimpleName(), idMap);
		}
		return field;
	}
	
}
