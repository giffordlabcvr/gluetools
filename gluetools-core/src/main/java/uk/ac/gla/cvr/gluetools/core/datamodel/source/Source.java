package uk.ac.gla.cvr.gluetools.core.datamodel.source;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Source;

// TODO have a default source which always exists. Command arguments which require a source-name default to this.
// TODO add description fields where necessary to this and other object types. Update creation commands as neccessary. Allow set description as necessary.
@GlueDataClass(defaultListedProperties = {_Source.NAME_PROPERTY})
public class Source extends _Source {

	public static Map<String, String> pkMap(String name) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(NAME_PROPERTY, name);
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setName(pkMap.get(NAME_PROPERTY));
	}
	
	@Override
	public Map<String, String> pkMap() {
		return pkMap(getName());
	}

}