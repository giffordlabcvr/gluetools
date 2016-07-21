package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.command.project.module.ModulePluginCommand;

public abstract class AbstractPlaceCommand extends ModulePluginCommand<PlaceCommandResult, MaxLikelihoodPlacer> {

	protected PlaceCommandResult generatePlaceCommandResult(Map<String, List<PlacementResult>> seqNameToPlacementResults) {
		List<PlacementResult> flattenedPlacementResults = new ArrayList<PlacementResult>();
		seqNameToPlacementResults.forEach( (seqName, placeResults) -> {flattenedPlacementResults.addAll(placeResults);} );
		return new PlaceCommandResult(flattenedPlacementResults);
	}

}
