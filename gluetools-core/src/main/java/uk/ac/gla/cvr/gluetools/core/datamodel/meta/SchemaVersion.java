package uk.ac.gla.cvr.gluetools.core.datamodel.meta;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.auto._SchemaVersion;

public class SchemaVersion extends _SchemaVersion {

	public static final String currentVersionString = "4";
	
	public static Map<String, String> pkMap(Integer id) {
		Map<String, String> idMap = new LinkedHashMap<String, String>();
		idMap.put(ID_PROPERTY, Integer.toString(id));
		return idMap;
	}

	@Override
	public void setPKValues(Map<String, String> pkMap) {
		setId(Integer.parseInt(pkMap.get(ID_PROPERTY)));
	}

	public static boolean isLaterThanCurrent(String dbVersionString) {
		Integer[] dbVersion = versionStringToIntArray(dbVersionString);
		Integer[] currentVersion = versionStringToIntArray(currentVersionString);
		for(int i = 0; i < Math.min(currentVersion.length, dbVersion.length); i++) {
			if(dbVersion[i] > currentVersion[i]) {
				return true; // e.g. "1.2.3" is later than "1.1.3"
			}
		}
		if(dbVersion.length > currentVersion.length && dbVersionString.startsWith(currentVersionString)) {
			return true; // e.g. "1.1.4" is later than "1.1"
		}
		return false;
	}

	private static Integer[] versionStringToIntArray(String versionString) {
		String[] versionBits = versionString.split("\\.");
		return Arrays.asList(versionBits).stream().map(b -> Integer.parseInt(b)).toArray(s->new Integer[s]);
	}
	
	@Override
	protected Map<String, String> pkMap() {
		return pkMap(getId());
	}

	
}
