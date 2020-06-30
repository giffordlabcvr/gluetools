package uk.ac.gla.cvr.gluetools.core.treeVisualiser;

import uk.ac.gla.cvr.gluetools.core.command.result.MapResult;

public class TreeDocumentToNewickResult extends MapResult {

	public TreeDocumentToNewickResult(String newickString) {
		super("treeDocumentToNewickResult", mapBuilder()
				.put("newickString", newickString)
		);
	}
	
}
