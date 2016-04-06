package uk.ac.gla.cvr.gluetools.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VersionUtils {

	
	public static int compareVersions(String version1, String version2) {
		int[] ints1 = versionStringToInts(version1);
		int[] ints2 = versionStringToInts(version2);
		int comp;
		comp = Integer.compare(ints1[0], ints2[0]);
		if(comp != 0) { return comp; }
		comp = Integer.compare(ints1[1], ints2[1]);
		if(comp != 0) { return comp; }
		comp = Integer.compare(ints1[2], ints2[2]);
		return comp;
	}
	
	
	private static int[] versionStringToInts(String versionString) {
		int[] versionInts = new int[3];
		Pattern pattern = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+).*");
		Matcher matcher = pattern.matcher(versionString);
		if(!matcher.find()) {
			throw new VersionUtilsException(VersionUtilsException.Code.CANNOT_EXTRACT_NUMBERS_FROM_VERSION_STRING, versionString);
		}
		versionInts[0] = Integer.parseInt(matcher.group(1));
		versionInts[1] = Integer.parseInt(matcher.group(2));
		versionInts[2] = Integer.parseInt(matcher.group(3));
		return versionInts;
	}
	
}
