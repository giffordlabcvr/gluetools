/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.datamodel.meta;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.datamodel.auto._SchemaVersion;

public class SchemaVersion extends _SchemaVersion {

	public static final String currentVersionString = "18";
	
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
	public Map<String, String> pkMap() {
		return pkMap(getId());
	}

	
}
