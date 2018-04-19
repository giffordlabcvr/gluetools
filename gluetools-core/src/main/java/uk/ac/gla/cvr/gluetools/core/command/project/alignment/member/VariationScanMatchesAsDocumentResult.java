package uk.ac.gla.cvr.gluetools.core.command.project.alignment.member;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.result.CommandResult;
import uk.ac.gla.cvr.gluetools.core.document.CommandArray;
import uk.ac.gla.cvr.gluetools.core.document.CommandDocument;
import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.core.variationscanner.VariationScanResult;

public class VariationScanMatchesAsDocumentResult extends CommandResult {

	public VariationScanMatchesAsDocumentResult(List<VariationScanResult<?>> vsrs) {
		super("variationScanMatchCommandResult");
		buildDocumentFromScanResults(getCommandDocument(), vsrs);
	}

	private void buildDocumentFromScanResults(CommandDocument commandDocument, List<VariationScanResult<?>> vsrs) {
		CommandArray variationsArray = commandDocument.setArray("variations");
		for(VariationScanResult<?> vsr: vsrs) {
			CommandObject variationObject = variationsArray.addObject();
			VariationScanResult.variationScanResultAsCommandObject(variationObject, vsr);
		}
	}

	
}
