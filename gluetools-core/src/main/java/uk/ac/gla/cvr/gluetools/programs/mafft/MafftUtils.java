package uk.ac.gla.cvr.gluetools.programs.mafft;

public class MafftUtils {

	public static String 
		MAFFT_EXECUTABLE_PROPERTY = "gluetools.core.programs.raxml.mafft.executable"; 

	public static String 
		MAFFT_TEMP_DIR_PROPERTY = "gluetools.core.programs.mafft.temp.dir";

	public static String 
		MAFFT_NUMBER_CPUS = "gluetools.core.programs.mafft.raxmlhpc.cpus";

	
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
