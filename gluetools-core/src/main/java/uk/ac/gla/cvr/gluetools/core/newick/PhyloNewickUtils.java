package uk.ac.gla.cvr.gluetools.core.newick;

import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;

public class PhyloNewickUtils {

	public static String phyloTreeToNewick(PhyloTree phyloTree) {
		NewickPhyloTreeVisitor newickPhyloTreeVisitor = new NewickPhyloTreeVisitor();
		phyloTree.accept(newickPhyloTreeVisitor);
		return newickPhyloTreeVisitor.getNewickString();
	}
	
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
