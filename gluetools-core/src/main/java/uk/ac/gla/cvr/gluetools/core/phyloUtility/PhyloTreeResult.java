package uk.ac.gla.cvr.gluetools.core.phyloUtility;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.document.PhyloTreeToDocumentTransformer;

public class PhyloTreeResult extends CommandResult {

	public PhyloTreeResult(PhyloTree phyloTree) {
		super(phyloTreeToCommandDocument(phyloTree));
	}
	
	private static CommandDocument phyloTreeToCommandDocument(PhyloTree phyloTree) {
		PhyloTreeToDocumentTransformer phyloTreeToDocumentTransformer = new PhyloTreeToDocumentTransformer();
		phyloTree.accept(phyloTreeToDocumentTransformer);
		return phyloTreeToDocumentTransformer.getDocument();
	}

}
