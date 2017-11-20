package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

@CommandClass(
		commandWords={"list", "placement"}, 
		description = "Summarise the placements for a single query in a result file", 
		docoptUsages = { "-i <inputFile> -q <queryName>" },
		docCategory = "Type-specific module commands",
		docoptOptions = { 
				"-i <inputFile>, --inputFile <inputFile>  Placement results file",
				"-q <queryName>, --queryName <queryName>  Query sequence name",
		},
		furtherHelp = "",
		metaTags = {CmdMeta.consoleOnly}	
)
public class ListPlacementCommand extends AbstractQueryResultCommand<ListPlacementCommand.Result> {


	@Override
	protected Result executeOnQueryResult(CommandContext cmdContext,
			MaxLikelihoodPlacer maxLikelihoodPlacer,
			MaxLikelihoodPlacerResult placerResult,
			MaxLikelihoodSingleQueryResult queryResult) {
		return new Result(queryResult.singlePlacement);
	}

	
	public static class Result extends BaseTableResult<MaxLikelihoodSinglePlacement> {
		public Result(List<MaxLikelihoodSinglePlacement> rowObjects) {
			super("listPlacementResult", rowObjects, 
					column("placementIndex", singlePlacement -> singlePlacement.placementIndex),
					column("likeWeightRatio", singlePlacement -> singlePlacement.likeWeightRatio),
					column("edgeIndex", singlePlacement -> singlePlacement.edgeIndex),
					column("distalLength", singlePlacement -> singlePlacement.distalLength),
					column("pendantLength", singlePlacement -> singlePlacement.pendantLength),
					column("logLikelihood", singlePlacement -> singlePlacement.logLikelihood));
		}
		
	}

	@CompleterClass
	public static class Completer extends AbstractQueryResultCommandCompleter {}

	
	
}
