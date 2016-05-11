package uk.ac.gla.cvr.gluetools.core.datamodel.moduleResource;

import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.auto._Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.auto._ModuleResource;

public class ModuleResource extends _ModuleResource {
	
	public static final String MODULE_NAME_PATH = 
			_ModuleResource.MODULE_PROPERTY+"."+_Module.NAME_PROPERTY;
	
	public static Map<String, String> pkMap(String moduleName, String name) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(MODULE_NAME_PATH, moduleName);
		idMap.put(NAME_PROPERTY, name);
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setName(pkMap.get(NAME_PROPERTY));
	}
	
	@Override
	public Map<String, String> pkMap() {
		return pkMap(
				getModule().getName(),
				getName());
	}
	
}
