package uk.ac.gla.cvr.gluetools.core.datamodel.project;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Sequence;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Source;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.Field;

@GlueDataClass(defaultListColumns = {_Project.NAME_PROPERTY, _Project.DESCRIPTION_PROPERTY})
public class Project extends _Project {

	public static Map<String, String> pkMap(String name) {
		return Collections.singletonMap(NAME_PK_COLUMN, name);
	}

	@Override
	public void setPKValues(Map<String, String> idMap) {
		setName(idMap.get(NAME_PK_COLUMN));
	}
	
	public Field getSequenceField(String fieldName) {
		return getFields().stream().filter(f -> f.getName().equals(fieldName)).findFirst().get();
	}

	public List<String> getCustomSequenceFieldNames() {
		return getFields().stream().map(Field::getName).collect(Collectors.toList());
	}

	public List<String> getAllSequenceFieldNames() {
		List<String> fieldNames = getCustomSequenceFieldNames();
		fieldNames.add(_Sequence.SEQUENCE_ID_PROPERTY);
		fieldNames.add(_Sequence.SOURCE_PROPERTY+"."+_Source.NAME_PROPERTY);
		fieldNames.add(_Sequence.FORMAT_PROPERTY);
		return fieldNames;
	}

	
}
