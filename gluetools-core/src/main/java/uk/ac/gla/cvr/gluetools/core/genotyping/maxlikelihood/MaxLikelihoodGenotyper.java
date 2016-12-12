package uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood;

import java.io.File;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.cayenne.query.SelectQuery;
import org.biojava.nbio.core.sequence.DNASequence;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.module.Module;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodPlacer;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodPlacer.PlacerResultInternal;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodSinglePlacement;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodSingleQueryResult;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.PlacementNeighbour;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.PlacementNeighbourFinder;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@PluginClass(elemName="maxLikelihoodGenotyper")
public class MaxLikelihoodGenotyper extends ModulePlugin<MaxLikelihoodGenotyper> {

	public static final String MAX_LIKELIHOOD_PLACER_MODULE_NAME = "maxLikelihoodPlacerModuleName";
	
	private String maxLikelihoodPlacerModuleName;
	private List<CladeCategory> cladeCategories;
	
	public MaxLikelihoodGenotyper() {
		super();
		addModulePluginCmdClass(GenotypeFileCommand.class);
		addModulePluginCmdClass(GenotypeSequenceCommand.class);
		addModulePluginCmdClass(GenotypePlacerResultCommand.class);
		addSimplePropertyName(MAX_LIKELIHOOD_PLACER_MODULE_NAME);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		this.maxLikelihoodPlacerModuleName = PluginUtils.configureStringProperty(configElem, MAX_LIKELIHOOD_PLACER_MODULE_NAME, true);
		List<Element> categoryElems = PluginUtils.findConfigElements(configElem, "cladeCategory");
		this.cladeCategories = PluginFactory.createPlugins(pluginConfigContext, CladeCategory.class, categoryElems);
	}

	
	@Override
	public void validate(CommandContext cmdContext) {
		super.validate(cmdContext);
		MaxLikelihoodPlacer maxLikelihoodPlacer = resolvePlacer(cmdContext);
		maxLikelihoodPlacer.validate(cmdContext);
	}

	public Map<String, QueryGenotypingResult> genotype(CommandContext cmdContext, Map<String, DNASequence> querySequenceMap, File dataDirFile) {
		MaxLikelihoodPlacer maxLikelihoodPlacer = resolvePlacer(cmdContext);
		PhyloTree glueProjectPhyloTree = maxLikelihoodPlacer.constructGlueProjectPhyloTree(cmdContext);
		PlacerResultInternal placerResult = maxLikelihoodPlacer.place(cmdContext, glueProjectPhyloTree, querySequenceMap, dataDirFile);
		Map<Integer, PhyloBranch> edgeIndexToPhyloBranch = placerResult.getEdgeIndexToPhyloBranch();
		Collection<MaxLikelihoodSingleQueryResult> singleQueryResults = placerResult.getQueryResults().values();
		return genotype(cmdContext, glueProjectPhyloTree, edgeIndexToPhyloBranch, singleQueryResults);
	}

	public Map<String, QueryGenotypingResult> genotype(CommandContext cmdContext,
			PhyloTree glueProjectPhyloTree,
			Map<Integer, PhyloBranch> edgeIndexToPhyloBranch,
			Collection<MaxLikelihoodSingleQueryResult> singleQueryResults) {
		Map<String, QueryGenotypingResult> queryGenotypingResults = new LinkedHashMap<String, QueryGenotypingResult>();
		Map<String, PlacementNeighbour> cladeToClosestNeighbour = new LinkedHashMap<String, PlacementNeighbour>();
		for(MaxLikelihoodSingleQueryResult queryResult: singleQueryResults) {
			QueryGenotypingResult queryGenotypingResult = new QueryGenotypingResult();
			queryGenotypingResults.put(queryResult.queryName, queryGenotypingResult);
			queryGenotypingResult.queryName = queryResult.queryName;
			for(CladeCategory cladeCategory: cladeCategories) {
				QueryCladeCategoryResult queryCladeCategoryResult = new QueryCladeCategoryResult();
				queryGenotypingResult.queryCladeCategoryResult.add(queryCladeCategoryResult);
				queryCladeCategoryResult.categoryName = cladeCategory.getName();
				queryCladeCategoryResult.categoryDisplayName = cladeCategory.getDisplayName();
				
				Double distanceCutoff = cladeCategory.getDistanceCutoff();
				Double distanceScalingExponent = cladeCategory.getDistanceScalingExponent();

				SelectQuery cladeCategoryAlmtSelectQuery = new SelectQuery(Alignment.class, cladeCategory.getWhereClause());
				Map<String, Alignment> cladeCategoryAlmtNameToAlmt = 
						GlueDataObject.query(cmdContext, Alignment.class, cladeCategoryAlmtSelectQuery).stream()
						.collect(Collectors.toMap(x -> x.getName(), x -> x));
				Map<String, Double> almtNameToScaledDistanceTotal = new LinkedHashMap<String, Double>();
				Double allNeighboursScaledDistanceTotal = 0.0;
				
				for(MaxLikelihoodSinglePlacement placement: queryResult.singlePlacement) {
					PhyloLeaf placementLeaf = MaxLikelihoodPlacer
							.addPlacementToPhylogeny(glueProjectPhyloTree, edgeIndexToPhyloBranch, queryResult, placement);
					List<PlacementNeighbour> neighbours = PlacementNeighbourFinder.findNeighbours(placementLeaf, new BigDecimal(distanceCutoff), null);
					for(PlacementNeighbour neighbour: neighbours) {
						BigDecimal distance = neighbour.getDistance();
						Double scaledDistance = Math.pow(distance.doubleValue(), distanceScalingExponent) * placement.likeWeightRatio;
						
						String neighbourLeafName = neighbour.getPhyloLeaf().getName();
						Map<String,String> neighbourMemberPkMap = Project.targetPathToPkMap(ConfigurableTable.alignment_member, neighbourLeafName);
						AlignmentMember neighbourAlmtMember = GlueDataObject.lookup(cmdContext, AlignmentMember.class, neighbourMemberPkMap);
						List<Alignment> neighbourAncestors = neighbourAlmtMember.getAlignment().getAncestors();
						for(Alignment neighbourAncestor: neighbourAncestors) {
							String neighbourAncestorName = neighbourAncestor.getName();
							if(cladeCategoryAlmtNameToAlmt.containsKey(neighbourAncestorName)) {
								PlacementNeighbour cladeClosestNeighbour = cladeToClosestNeighbour.get(neighbourAncestorName);
								if(cladeClosestNeighbour == null || neighbour.getDistance().compareTo(cladeClosestNeighbour.getDistance()) < 0) {
									cladeToClosestNeighbour.put(neighbourAncestorName, neighbour);
								}
								allNeighboursScaledDistanceTotal = allNeighboursScaledDistanceTotal + scaledDistance;
								Double currentTotal = almtNameToScaledDistanceTotal.get(neighbourAncestorName);
								if(currentTotal == null) {
									currentTotal = 0.0;
								}
								currentTotal = currentTotal + scaledDistance;
								almtNameToScaledDistanceTotal.put(neighbourAncestorName, currentTotal);
								break;
							}
						}
					}
					MaxLikelihoodPlacer.removePlacementFromPhylogeny(placementLeaf);
				}
				for(Map.Entry<String, Double> entry: almtNameToScaledDistanceTotal.entrySet()) {
					String almtName = entry.getKey();
					Double scaledDistanceTotal = entry.getValue();
					if(allNeighboursScaledDistanceTotal == 0.0) {
						continue;
					}
					QueryCladeResult queryCladeResult = new QueryCladeResult();
					queryCladeCategoryResult.queryCladeResult.add(queryCladeResult);
					queryCladeResult.cladeName = almtName;
					queryCladeResult.cladeRenderedName = cladeCategoryAlmtNameToAlmt.get(almtName).getRenderedName();
					queryCladeResult.percentScore = 
							(scaledDistanceTotal / allNeighboursScaledDistanceTotal ) * 100.0;
					if(queryCladeResult.percentScore >= cladeCategory.getFinalCladeCutoff()) {
						queryCladeCategoryResult.finalClade = almtName;
						queryCladeCategoryResult.finalCladeRenderedName = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(almtName)).getRenderedName();
						PlacementNeighbour closestNeighbourWithinFinalClade = cladeToClosestNeighbour.get(almtName);
						if(closestNeighbourWithinFinalClade != null) {
							Map<String,String> closestMemberPkMap = 
									Project.targetPathToPkMap(ConfigurableTable.alignment_member, 
											closestNeighbourWithinFinalClade.getPhyloLeaf().getName());
							queryCladeCategoryResult.closestMemberAlignmentName = closestMemberPkMap.get(AlignmentMember.ALIGNMENT_NAME_PATH);
							queryCladeCategoryResult.closestMemberSourceName = closestMemberPkMap.get(AlignmentMember.SOURCE_NAME_PATH);
							queryCladeCategoryResult.closestMemberSequenceID = closestMemberPkMap.get(AlignmentMember.SEQUENCE_ID_PATH);
						}
					}
				}
			}
		}
		return queryGenotypingResults;
	}
	
	public MaxLikelihoodPlacer resolvePlacer(CommandContext cmdContext) {
		MaxLikelihoodPlacer maxLikelihoodPlacer = 
				Module.resolveModulePlugin(cmdContext, MaxLikelihoodPlacer.class, maxLikelihoodPlacerModuleName);
		return maxLikelihoodPlacer;
	}

	public List<CladeCategory> getCladeCategories() {
		return cladeCategories;
	}
	
	
}
