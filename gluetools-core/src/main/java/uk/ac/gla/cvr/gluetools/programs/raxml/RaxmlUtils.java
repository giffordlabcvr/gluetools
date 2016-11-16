package uk.ac.gla.cvr.gluetools.programs.raxml;


public class RaxmlUtils {

	public static String 
		RAXMLHPC_EXECUTABLE_PROPERTY = "gluetools.core.programs.raxml.raxmlhpc.executable"; 

	public static String 
		RAXML_TEMP_DIR_PROPERTY = "gluetools.core.programs.raxml.temp.dir";

	public static String 
		RAXMLHPC_NUMBER_CPUS = "gluetools.core.programs.raxml.raxmlhpc.cpus";

	
	public static boolean validRaxmlName(String string) {
		if(string.length() < 1 || string.length() > 256) {
			return false;
		}
		if(!string.matches("[^ \t\n\r\f:\\(\\)\\[\\]]+")) {
			return false;
		}
		return true;
	}

	
}
