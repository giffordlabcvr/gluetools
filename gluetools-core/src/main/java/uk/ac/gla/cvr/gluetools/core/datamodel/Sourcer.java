package uk.ac.gla.cvr.gluetools.core.datamodel;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;

import uk.ac.gla.cvr.gluetools.core.datamodel.DataModelException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Sourcer;

@GlueDataClass(listColumnHeaders = {"Name"})
public class Sourcer extends _Sourcer {

	@Override
	public String[] populateListRow() {
		return new String[]{getName()};
	}

	public String getName() {
		return getObjectId().getIdSnapshot().get(NAME_PK_COLUMN).toString();
	}

	public static Sourcer lookupSourcer(ObjectContext objContext, String projectId, String name) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(PROJECT_PK_COLUMN, projectId);
		idMap.put(NAME_PK_COLUMN, name);
		Sourcer populator = Cayenne.objectForPK(objContext, Sourcer.class, idMap);
		if(populator == null) {
			throw new DataModelException(Code.OBJECT_NOT_FOUND, Sourcer.class.getSimpleName(), idMap);
		}
		return populator;
	}
	
}
