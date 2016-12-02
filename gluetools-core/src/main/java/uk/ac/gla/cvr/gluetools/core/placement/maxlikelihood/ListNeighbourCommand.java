package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CmdMeta;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;

@CommandClass(
		commandWords={"list", "neighbour"}, 
		description = "List the neighbours of a placement in order of decreasing distance", 
		docoptUsages = { "-i <inputFile> -q <queryName> -p <placementIndex>" },
		docoptOptions = { 
				"-i <inputFile>, --inputFile <inputFile>                 Placement results file",
				"-q <queryName>, --queryName <queryName>                 Query sequence name",
				"-p <placementIndex>, --placementIndex <placementIndex>  Placement index"
		},
		furtherHelp = "",
		metaTags = {CmdMeta.consoleOnly}	
)
public class ListNeighbourCommand extends AbstractPlacementCommand<ListNeighbourCommand.Result> {

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
	}

	@Override
	protected Result executeOnPlacementResult(CommandContext cmdContext,
			MaxLikelihoodPlacer maxLikelihoodPlacer,
			MaxLikelihoodPlacerResult placerResult,
			MaxLikelihoodSingleQueryResult queryResult,
			MaxLikelihoodSinglePlacement placement) {
		PhyloTree glueProjectPhyloTree = maxLikelihoodPlacer.constructGlueProjectPhyloTree(cmdContext);
		Map<Integer, PhyloBranch> edgeIndexToPhyloBranch = maxLikelihoodPlacer.generateEdgeIndexToPhyloBranch(placerResult, glueProjectPhyloTree);
		PhyloLeaf placementLeaf = maxLikelihoodPlacer.addPlacementToPhylogeny(glueProjectPhyloTree, edgeIndexToPhyloBranch, queryResult, placement);
		List<ResultRow> resultRows = PlacementNeighbourFinder.findNeighbours(placementLeaf)
				.stream()
				.map(plcmtNeighbour -> {
					String leafName = plcmtNeighbour.getPhyloLeaf().getName();
					Map<String,String> memberPkMap = Project.targetPathToPkMap(ConfigurableTable.alignment_member, leafName);
					ResultRow resultRow = new ResultRow();
					resultRow.alignmentName = memberPkMap.get(AlignmentMember.ALIGNMENT_NAME_PATH);
					resultRow.sourceName = memberPkMap.get(AlignmentMember.SOURCE_NAME_PATH);
					resultRow.sequenceID = memberPkMap.get(AlignmentMember.SEQUENCE_ID_PATH);
					resultRow.distance = plcmtNeighbour.getDistance().doubleValue();
					return resultRow;
				})
				.collect(Collectors.toList());
		return new Result(resultRows);
	}
	
	private static class ResultRow {
		String alignmentName;
		String sourceName;
		String sequenceID;
		Double distance;
	}
	
	public static class Result extends BaseTableResult<ResultRow> {

		private Result(List<ResultRow> rowObjects) {
			super("listNeighbourResult", rowObjects, 
					column("alignmentName", plNe -> plNe.alignmentName), 
					column("sourceName", plNe -> plNe.sourceName), 
					column("sequenceID", plNe -> plNe.sequenceID), 
					column("distance", plNe -> plNe.distance));
		}
		
	}
	
	@CompleterClass
	public static class Completer extends AbstractPlacementCommandCompleter {
		public Completer() {
			super();
		}
		
	}


	
	
}
