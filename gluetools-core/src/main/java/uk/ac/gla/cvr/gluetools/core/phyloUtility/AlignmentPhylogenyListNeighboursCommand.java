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
package uk.ac.gla.cvr.gluetools.core.phyloUtility;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.AdvancedCmdCompleter;
import uk.ac.gla.cvr.gluetools.core.command.Command;
import uk.ac.gla.cvr.gluetools.core.command.CommandClass;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.CompleterClass;
import uk.ac.gla.cvr.gluetools.core.command.CompletionSuggestion;
import uk.ac.gla.cvr.gluetools.core.command.console.ConsoleCommandContext;
import uk.ac.gla.cvr.gluetools.core.command.project.InsideProjectMode;
import uk.ac.gla.cvr.gluetools.core.command.result.BaseTableResult;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.field.FieldType;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeVisitor;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;
import uk.ac.gla.cvr.gluetools.core.treerenderer.PhyloExporter;

@CommandClass(
		commandWords={"alignment-phylogeny", "list", "neighbours"}, 
		description = "Find neighbours within phylogenetic tree associated with an alignment.", 
		docoptUsages = { "<alignmentName> <fieldName> [-w <whereClause>] [-n <maxNeighbours>] [-d <maxDistance>]"},
		docoptOptions = { 
				"-w <whereClause>, --whereClause <whereClause>        Specify starting alignment members",
				"-n <maxNeighbours>, --maxNeighbours <maxNeighbours>  Max number of neighbours per starting taxon",
				"-d <maxDistance>, --maxDistance <maxDistance>  Max distance between starting and neighbour",
		},
		furtherHelp = "List the neareast neighbours of each of the taxa specified by <whereClause>",
		metaTags = {}	
)
public class AlignmentPhylogenyListNeighboursCommand extends PhyloUtilityCommand<AlignmentPhylogenyListNeighboursCommand.Result> {

	public static final String ALIGNMENT_NAME = "alignmentName";
	public static final String FIELD_NAME = "fieldName";
	public static final String WHERE_CLAUSE = "whereClause";
	public static final String MAX_NEIGHBOURS = "maxNeighbours";
	public static final String MAX_DISTANCE = "maxDistance";
	
	private Expression whereClause;
	private String alignmentName;
	private String fieldName;
	private Integer maxNeighbours;
	private Double maxDistance;
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.alignmentName = PluginUtils.configureStringProperty(configElem, ALIGNMENT_NAME, true);
		this.fieldName = PluginUtils.configureStringProperty(configElem, FIELD_NAME, true);
		this.whereClause = PluginUtils.configureCayenneExpressionProperty(configElem, WHERE_CLAUSE, false);
		this.maxNeighbours = PluginUtils.configureIntProperty(configElem, MAX_NEIGHBOURS, 1, true, null, false, false);
		this.maxDistance = PluginUtils.configureDoubleProperty(configElem, MAX_DISTANCE, 0.0, false, null, false, false);
	}

	@Override
	protected Result execute(CommandContext cmdContext, PhyloUtility phyloUtility) {
		Alignment alignment = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(alignmentName));
		Project project = ((InsideProjectMode) cmdContext.peekCommandMode()).getProject();
		project.checkProperty(ConfigurableTable.alignment.name(), fieldName, EnumSet.of(FieldType.VARCHAR, FieldType.CLOB), false);
		PhyloTree phyloTree = 
				PhyloExporter.exportAlignmentPhyloTree(cmdContext, alignment, fieldName, false);
	
		Expression memberExp = ExpressionFactory.matchExp(AlignmentMember.ALIGNMENT_NAME_PATH, alignment.getName());
		if(whereClause != null) {
			memberExp = memberExp.andExp(whereClause);
		}
		
		
		List<AlignmentMember> almtMembers = GlueDataObject.query(cmdContext, AlignmentMember.class, new SelectQuery(AlignmentMember.class, memberExp));
		Set<Map<String, String>> almtMemberPkMaps = new LinkedHashSet<Map<String, String>>();
		almtMembers.forEach(memb -> almtMemberPkMaps.add(memb.pkMap()));
		Map<Map<String, String>, PhyloLeaf> memberPkMapToLeaf = new LinkedHashMap<Map<String, String>, PhyloLeaf>();
		
		phyloTree.accept(new PhyloTreeVisitor() {
			@Override
			public void visitLeaf(PhyloLeaf phyloLeaf) {
				Map<String, String> leafPkMap = Project.targetPathToPkMap(ConfigurableTable.alignment_member, phyloLeaf.getName());
				if(almtMemberPkMaps.remove(leafPkMap)) {
					memberPkMapToLeaf.put(leafPkMap, phyloLeaf);
				}
			}
		});
	
		List<ResultRow> resultRows = new ArrayList<ResultRow>();
		memberPkMapToLeaf.forEach((startPkMap, startLeaf) -> {
			BigDecimal distanceCutoff = null;
			if(maxDistance != null) {
				distanceCutoff = new BigDecimal(maxDistance);
			}
			List<PhyloNeighbour> phyloNeighbours = PhyloNeighbourFinder.findNeighbours(startLeaf, distanceCutoff, maxNeighbours);
			phyloNeighbours.forEach(phyloNeighbour -> {
				resultRows.add(new ResultRow(
						new LinkedHashMap<String,String>(startPkMap), 
						phyloNeighbour.getRank(), 
						phyloNeighbour.getDistance().doubleValue(), 
						Project.targetPathToPkMap(ConfigurableTable.alignment_member, phyloNeighbour.getPhyloLeaf().getName())
				));
			});
			
		});
		return new Result(resultRows);
	}

	private static class ResultRow {
		private Map<String, String> startMemberPkMap;
		private Map<String, String> neighbourMemberPkMap;
		private int rank;
		private double distance;
		public ResultRow(Map<String, String> startMemberPkMap, int rank, double distance,
				Map<String, String> neighbourMemberPkMap) {
			super();
			this.startMemberPkMap = startMemberPkMap;
			this.rank = rank;
			this.distance = distance;
			this.neighbourMemberPkMap = neighbourMemberPkMap;
		}
		
	}
	
	public static class Result extends BaseTableResult<ResultRow> {

		private Result(List<ResultRow> rowObjects) {
			super("listNeighbourResult", rowObjects, 
					column("startSourceName", resultRow -> resultRow.startMemberPkMap.get(AlignmentMember.SOURCE_NAME_PATH)), 
					column("startSequenceID", resultRow -> resultRow.startMemberPkMap.get(AlignmentMember.SEQUENCE_ID_PATH)), 
					column("rank", resultRow -> resultRow.rank), 
					column("distance", resultRow -> resultRow.distance), 
					column("neighbourSourceName", resultRow -> resultRow.neighbourMemberPkMap.get(AlignmentMember.SOURCE_NAME_PATH)), 
					column("neighbourSequenceID", resultRow -> resultRow.neighbourMemberPkMap.get(AlignmentMember.SEQUENCE_ID_PATH)));
		}
		
	}
	
	@CompleterClass
	public static class Completer extends AdvancedCmdCompleter {
		public Completer() {
			super();
			registerDataObjectNameLookup("alignmentName", Alignment.class, Alignment.NAME_PROPERTY);
			registerVariableInstantiator("fieldName", new AdvancedCmdCompleter.VariableInstantiator() {
					@Override
					public List<CompletionSuggestion> instantiate(
							ConsoleCommandContext cmdContext,
							@SuppressWarnings("rawtypes") Class<? extends Command> cmdClass, Map<String, Object> bindings,
							String prefix) {
						InsideProjectMode insideProjectMode = (InsideProjectMode) cmdContext.peekCommandMode();
						Project project = insideProjectMode.getProject();
						List<String> listableFieldNames = project.getModifiableFieldNames(ConfigurableTable.alignment.name());
						return listableFieldNames.stream().map(n -> new CompletionSuggestion(n, true)).collect(Collectors.toList());
					}
			});
		}
		
	}
	
}
