package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentUtils;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.document.DocumentToPhyloTreeTransformer;
import uk.ac.gla.cvr.gluetools.core.phylotree.document.PhyloTreeToDocumentTransformer;

/**
 * Similar to the original MaxLikelihoodPlacerResult but 
 * 
 * (a) uses custom methods to convert to/from command document format, rather than generic pojo-based functionality.
 * (b) renders its labelledPhyloTree as a command document, allowing detailed information attached to internal nodes etc.
 *
 */
public class DetailedMaxLikelihoodPlacerResult implements IMaxLikelihoodPlacerResult {

	public static final String DOCUMENT_ROOT_NAME = "detailedMaxLikelihoodPlacerResult";
	private PhyloTree labelledPhyloTree;
	private List<MaxLikelihoodSingleQueryResult> queryResults;

	@Override
	public PhyloTree getLabelledPhyloTree() {
		return labelledPhyloTree;
	}

	@Override
	public List<MaxLikelihoodSingleQueryResult> getQueryResults() {
		return queryResults;
	}

	public DetailedMaxLikelihoodPlacerResult(PhyloTree labelledPhyloTree,
			List<MaxLikelihoodSingleQueryResult> queryResults) {
		super();
		this.labelledPhyloTree = labelledPhyloTree;
		this.queryResults = queryResults;
	}

	public static CommandDocument toCommandDocument(DetailedMaxLikelihoodPlacerResult placerResult) {
		CommandDocument cmdDocument = new CommandDocument(DOCUMENT_ROOT_NAME);
		CommandArray queryResultArray = cmdDocument.setArray("queryResults");
		for(MaxLikelihoodSingleQueryResult queryResult: placerResult.queryResults) {
			PojoDocumentUtils.addToArray(queryResultArray, queryResult);
		}
		PhyloTreeToDocumentTransformer phyloTreeToDocumentTransformer = new PhyloTreeToDocumentTransformer();
		placerResult.labelledPhyloTree.accept(phyloTreeToDocumentTransformer);
		CommandDocument phyloTreeDoc = phyloTreeToDocumentTransformer.getDocument();
		cmdDocument.setObject("labelledPhyloTree", phyloTreeDoc);
		return cmdDocument;
	}

	public static DetailedMaxLikelihoodPlacerResult fromCommandDocument(CommandDocument cmdDocument) {
		CommandArray queryResultArray = cmdDocument.getArray("queryResults");
		List<MaxLikelihoodSingleQueryResult> queryResults = new ArrayList<MaxLikelihoodSingleQueryResult>();
		if(queryResultArray != null) {
			for(int i = 0; i < queryResultArray.size(); i++) {
				queryResults.add(PojoDocumentUtils.commandObjectToPojo(queryResultArray.getObject(i), MaxLikelihoodSingleQueryResult.class));
			}
		}
		CommandObject phyloTreeCmdObject = cmdDocument.getObject("labelledPhyloTree");
		CommandDocument phyloTreeCmdDocument = new CommandDocument("phyloTree");
		phyloTreeCmdDocument.shallowCopyFrom(phyloTreeCmdObject);
		DocumentToPhyloTreeTransformer documentToPhyloTreeTransformer = new DocumentToPhyloTreeTransformer();
		phyloTreeCmdDocument.accept(documentToPhyloTreeTransformer);
		PhyloTree labelledPhyloTree = documentToPhyloTreeTransformer.getPhyloTree();
		return new DetailedMaxLikelihoodPlacerResult(labelledPhyloTree, queryResults);
	}
	
}
