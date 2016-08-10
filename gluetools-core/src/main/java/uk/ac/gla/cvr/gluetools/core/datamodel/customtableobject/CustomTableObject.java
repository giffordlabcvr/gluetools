package uk.ac.gla.cvr.gluetools.core.datamodel.customtableobject;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.auto._CustomTableObject;

public abstract class CustomTableObject extends _CustomTableObject {

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setId(pkMap.get(ID_PROPERTY));
	}

	@Override
	public Map<String, String> pkMap() {
		return pkMap(getId());
	}

	public static Map<String, String> pkMap(String id) {
		Map<String, String> pkMap = new LinkedHashMap<String,String>();
		pkMap.put(ID_PROPERTY, id);
		return pkMap;
	}
	
}
