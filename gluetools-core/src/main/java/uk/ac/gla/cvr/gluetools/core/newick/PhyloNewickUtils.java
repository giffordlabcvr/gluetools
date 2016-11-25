package uk.ac.gla.cvr.gluetools.core.newick;


public class PhyloNewickUtils {

	public static boolean validNodeName(String nodeName) {
		if(nodeName.length() == 0) {
			return false;
		}
		if(nodeName.contains("(") || nodeName.contains(")") || nodeName.contains("[") || nodeName.contains("]") || 
				nodeName.contains(";") || nodeName.contains(",") || nodeName.contains(":")) {
			return false;
		}
		return true;
	}
	
}
