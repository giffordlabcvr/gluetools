/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
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
