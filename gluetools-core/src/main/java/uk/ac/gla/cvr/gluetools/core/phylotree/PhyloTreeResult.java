package uk.ac.gla.cvr.gluetools.core.phylotree;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.phylotree.document.PhyloTreeToDocumentTransformer;

public class PhyloTreeResult extends CommandResult {

	public PhyloTreeResult(PhyloTree phyloTree) {
		super(phyloTreeToDocument(phyloTree));
	}
	
	private static CommandDocument phyloTreeToDocument(PhyloTree phyloTree) {
		PhyloTreeToDocumentTransformer phyloTreeToDocumentTransformer = new PhyloTreeToDocumentTransformer();
		phyloTree.accept(phyloTreeToDocumentTransformer);
		return phyloTreeToDocumentTransformer.getDocument();
	}
}
