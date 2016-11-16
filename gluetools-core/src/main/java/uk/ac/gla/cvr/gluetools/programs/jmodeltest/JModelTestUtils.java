package uk.ac.gla.cvr.gluetools.programs.jmodeltest;

public class JModelTestUtils {

	public static String 
		JMODELTESTER_JAR_PROPERTY = "gluetools.core.programs.jmodeltester.jar"; 

	public static String 
		JMODELTESTER_TEMP_DIR_PROPERTY = "gluetools.core.programs.jmodeltester.temp.dir";

	public static String 
		JMODELTESTER_NUMBER_CPUS = "gluetools.core.programs.jmodeltester.cpus";

	public static boolean validPhyMLName(String string) {
		if(string.length() < 1 || string.length() > 100) {
			return false;
		}
		// disallow whitespace, comma, colon, '(', ')', '[' and ']'
		if(!string.matches("[^ \t\n\r\f,:\\(\\)\\[\\]]+")) {
			return false;
		}
		return true;
	}

	
}
