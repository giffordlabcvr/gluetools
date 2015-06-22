package uk.ac.gla.cvr.gluetools.core.datamodel.project;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Project;

@GlueDataClass(listColumnHeaders = {_Project.NAME_PROPERTY, _Project.DESCRIPTION_PROPERTY})
public class Project extends _Project {

	@Override
	public String[] populateListRow() {
		return new String[]{
				getName(),
				Optional.ofNullable(getDescription()).orElse("-")
		};
	}

	public static Map<String, String> pkMap(String name) {
		return Collections.singletonMap(NAME_PK_COLUMN, name);
	}

	@Override
	public void setPKValues(Map<String, String> idMap) {
		setName(idMap.get(NAME_PK_COLUMN));
	}
	
}
