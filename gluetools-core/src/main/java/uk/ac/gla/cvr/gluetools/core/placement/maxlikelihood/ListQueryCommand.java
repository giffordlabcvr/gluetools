package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import java.util.List;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;

@CommandClass(
		commandWords={"list", "query"}, 
		description = "Summarise the per-query placement results from a file", 
		docoptUsages = { "-i <inputFile>" },
		docCategory = "Type-specific module commands",
		docoptOptions = { 
				"-i <inputFile>, --inputFile <inputFile>  Placement results file",
		},
		furtherHelp = "",
		metaTags = {CmdMeta.consoleOnly}	
)
public class ListQueryCommand extends AbstractPlacerResultCommand<ListQueryCommand.Result> {

	@Override
	protected Result executeOnPlacerResult(CommandContext cmdContext, MaxLikelihoodPlacer maxLikelihoodPlacer, MaxLikelihoodPlacerResult placerResult) {
		return new Result(placerResult.singleQueryResult);
	}
	
	public static class Result extends BaseTableResult<MaxLikelihoodSingleQueryResult> {
		public Result(List<MaxLikelihoodSingleQueryResult> rowObjects) {
			super("listQueryResult", rowObjects, 
					column("queryName", singleQueryResult -> singleQueryResult.queryName),
					column("numPlacements", singleQueryResult -> singleQueryResult.singlePlacement.size()));
		}
		
	}

	@CompleterClass
	public static class Completer extends AbstractPlacerResultCommandCompleter {}
	
	
}
