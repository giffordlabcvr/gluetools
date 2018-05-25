package uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood;

import java.util.ArrayList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.core.document.pojo.PojoDocumentUtils;


public class GenotypingDocumentResult extends CommandResult {

	public GenotypingDocumentResult(List<CladeCategory> cladeCategories, ArrayList<QueryGenotypingResult> queryGenotypingResults) {
		super("genotypingDocumentResult");
		CommandArray queryGenotypingResultsArray = getCommandDocument().setArray("queryGenotypingResults");
		queryGenotypingResults.forEach(queryGenotypingResult -> {
			CommandObject queryGenotypingResultObj = queryGenotypingResultsArray.addObject();
			PojoDocumentUtils.setPojoProperties(queryGenotypingResultObj, queryGenotypingResult);
		});
	
	}

}
