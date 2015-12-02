package uk.ac.gla.cvr.gluetools.core.targetSet;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataClass;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._TargetSet;

@GlueDataClass(defaultListColumns = {_TargetSet.NAME_PROPERTY})
public class TargetSet extends _TargetSet {

	
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
	protected Map<String, String> pkMap() {
		return pkMap(getName());
	}
	
}
