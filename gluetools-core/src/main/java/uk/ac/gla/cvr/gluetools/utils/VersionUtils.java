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
package uk.ac.gla.cvr.gluetools.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.utils.VersionUtilsException.Code;

public class VersionUtils {

	
	public static int compareVersions(String version1, String version2) {
		int[] ints1 = parseVersionString(version1);
		int[] ints2 = parseVersionString(version2);
		int comp;
		comp = Integer.compare(ints1[0], ints2[0]);
		if(comp != 0) { return comp; }
		comp = Integer.compare(ints1[1], ints2[1]);
		if(comp != 0) { return comp; }
		comp = Integer.compare(ints1[2], ints2[2]);
		return comp;
	}
	
	
	public static int[] parseVersionString(String versionString) {
		int[] versionInts = new int[3];
		Pattern pattern = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+).*");
		Matcher matcher = pattern.matcher(versionString);
		if(!matcher.find()) {
			throw new VersionUtilsException(VersionUtilsException.Code.VERSION_STRING_INCORRECT_FORMAT, versionString);
		}
		versionInts[0] = Integer.parseInt(matcher.group(1));
		versionInts[1] = Integer.parseInt(matcher.group(2));
		versionInts[2] = Integer.parseInt(matcher.group(3));
		return versionInts;
	}


	public static void checkMinVersion(CommandContext cmdContext, String projectMinVersion) {
		String glueEngineVersion = cmdContext.getGluetoolsEngine().getGluecoreProperties().getProperty("version", null);
		if(glueEngineVersion != null && VersionUtils.compareVersions(glueEngineVersion, projectMinVersion) < 0) {
			throw new VersionUtilsException(Code.GLUE_ENGINE_VERSION_EARLIER_THAN_PROJECT_MIN, glueEngineVersion, projectMinVersion);
		}
	}

	public static void checkMaxVersion(CommandContext cmdContext, String projectMaxVersion) {
		String glueEngineVersion = cmdContext.getGluetoolsEngine().getGluecoreProperties().getProperty("version", null);
		if(glueEngineVersion != null && VersionUtils.compareVersions(glueEngineVersion, projectMaxVersion) > 0) {
			throw new VersionUtilsException(Code.GLUE_ENGINE_VERSION_LATER_THAN_PROJECT_MAX, glueEngineVersion, projectMaxVersion);
		}
	}
	
}
