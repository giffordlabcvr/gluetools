package uk.ac.gla.cvr.gluetools.core.datamodel;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;

import uk.ac.gla.cvr.gluetools.core.datamodel.DataModelException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Populator;

@GlueDataClass(listColumnHeaders = {"Name"})
public class Populator extends _Populator {

	@Override
	public String[] populateListRow() {
		return new String[]{getName()};
	}

	public String getName() {
		return getObjectId().getIdSnapshot().get(NAME_PK_COLUMN).toString();
	}

	public static Populator lookupPopulator(ObjectContext objContext, String projectId, String name) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(PROJECT_PK_COLUMN, projectId);
		idMap.put(NAME_PK_COLUMN, name);
		Populator populator = Cayenne.objectForPK(objContext, Populator.class, idMap);
		if(populator == null) {
			throw new DataModelException(Code.OBJECT_NOT_FOUND, Populator.class.getSimpleName(), idMap);
		}
		return populator;
	}

	
}
