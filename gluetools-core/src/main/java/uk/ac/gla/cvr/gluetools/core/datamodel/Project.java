package uk.ac.gla.cvr.gluetools.core.datamodel;

import java.util.Collections;
import java.util.Map;

import org.apache.cayenne.Cayenne;
import org.apache.cayenne.ObjectContext;

import uk.ac.gla.cvr.gluetools.core.datamodel.DataModelException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Project;

@GlueDataClass(listColumnHeaders = {"ID", "DisplayName"})
public class Project extends _Project {

	@Override
	public String[] populateListRow() {
		return new String[]{
				getObjectId().getIdSnapshot().get(ID_PK_COLUMN).toString(), 
				getDisplayName()};
	}

	public static Project lookupProject(ObjectContext objContext, String projectId) {
		Map<String, String> idMap = Collections.singletonMap(Project.ID_PK_COLUMN, projectId);
		Project project = Cayenne.objectForPK(objContext, Project.class, idMap);
		if(project == null) {
			throw new DataModelException(Code.OBJECT_NOT_FOUND, Project.class.getSimpleName(), idMap);
		}
		return project;
	}

	
}
