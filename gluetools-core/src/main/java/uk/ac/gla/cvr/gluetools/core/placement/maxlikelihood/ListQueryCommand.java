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
		commandWords={"list", "query"}, 
		description = "Summarise the per-query placement results from a file", 
		docoptUsages = { "-i <inputFile>" },
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
