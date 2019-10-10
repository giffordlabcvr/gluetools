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
package uk.ac.gla.cvr.gluetools.core.genotyping.maxlikelihood;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.apache.cayenne.query.SelectQuery;
import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.genotyping.BaseCladeCategory;
import uk.ac.gla.cvr.gluetools.core.genotyping.BaseGenotyper;
import uk.ac.gla.cvr.gluetools.core.genotyping.QueryCladeCategoryResult;
import uk.ac.gla.cvr.gluetools.core.genotyping.QueryCladeResult;
import uk.ac.gla.cvr.gluetools.core.genotyping.QueryGenotypingResult;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodPlacer;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodSinglePlacement;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodSingleQueryResult;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.PlacementNeighbour;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.PlacementNeighbourFinder;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginFactory;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


@PluginClass(elemName="maxLikelihoodGenotyper",
		description="Runs the neighbour-weighting phase of the maximum-likelihood clade assignment method")
public class MaxLikelihoodGenotyper extends BaseGenotyper<MaxLikelihoodGenotyper> {

	
	private List<MaxLikelihoodCladeCategory> cladeCategories;
	
	public MaxLikelihoodGenotyper() {
		super();
		registerModulePluginCmdClass(MaxLikelihoodGenotypeFileCommand.class);
		registerModulePluginCmdClass(MaxLikelihoodGenotypeSequenceCommand.class);
		registerModulePluginCmdClass(MaxLikelihoodGenotypeFastaDocumentCommand.class);
		registerModulePluginCmdClass(MaxLikelihoodGenotypePlacerResultCommand.class);
		registerModulePluginCmdClass(MaxLikelihoodGenotypePlacerResultDocumentCommand.class);
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		List<Element> categoryElems = PluginUtils.findConfigElements(configElem, "cladeCategory");
		this.cladeCategories = PluginFactory.createPlugins(pluginConfigContext, MaxLikelihoodCladeCategory.class, categoryElems);
	}

	@Override
	public Map<String, QueryGenotypingResult> genotype(CommandContext cmdContext,
			PhyloTree glueProjectPhyloTree,
			Map<Integer, PhyloBranch> edgeIndexToPhyloBranch,
			Collection<MaxLikelihoodSingleQueryResult> singleQueryResults) {
		log(Level.FINEST, "Genotyping "+singleQueryResults.size()+" placer results");
		int resultsComplete = 0;
		Map<String, QueryGenotypingResult> queryGenotypingResults = new LinkedHashMap<String, QueryGenotypingResult>();
		
		Map<String, List<String>> leafNameToAncestorAlmtNames = 
				referenceMemberAncestorAlmts(cmdContext, glueProjectPhyloTree);
		
		for(MaxLikelihoodSingleQueryResult queryResult: singleQueryResults) {
			QueryGenotypingResult queryGenotypingResult = new QueryGenotypingResult();
			queryGenotypingResults.put(queryResult.queryName, queryGenotypingResult);
			queryGenotypingResult.queryName = queryResult.queryName;
			for(MaxLikelihoodCladeCategory cladeCategory: cladeCategories) {
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
				
				Map<String, PlacementNeighbour> cladeToClosestNeighbour = new LinkedHashMap<String, PlacementNeighbour>();
				PlacementNeighbour closestTarget = null;
				
				for(MaxLikelihoodSinglePlacement placement: queryResult.singlePlacement) {
					PhyloLeaf placementLeaf = MaxLikelihoodPlacer
							.addPlacementToPhylogeny(glueProjectPhyloTree, edgeIndexToPhyloBranch, queryResult, placement);
					List<PlacementNeighbour> neighbours = PlacementNeighbourFinder.findNeighbours(placementLeaf, new BigDecimal(distanceCutoff), null);
					for(PlacementNeighbour neighbour: neighbours) {
						BigDecimal distance = neighbour.getDistance();
						Double scaledDistance = Math.pow(distance.doubleValue(), distanceScalingExponent) * placement.likeWeightRatio;
						
						String neighbourLeafName = neighbour.getPhyloLeaf().getName();
						List<String> neighbourAncestorAlmtNames = leafNameToAncestorAlmtNames.get(neighbourLeafName);
						for(String neighbourAncestorAlmtName: neighbourAncestorAlmtNames) {
							if(cladeCategoryAlmtNameToAlmt.containsKey(neighbourAncestorAlmtName)) {
								PlacementNeighbour cladeClosestNeighbour = cladeToClosestNeighbour.get(neighbourAncestorAlmtName);
								if(cladeClosestNeighbour == null || neighbour.getDistance().compareTo(cladeClosestNeighbour.getDistance()) < 0) {
									cladeToClosestNeighbour.put(neighbourAncestorAlmtName, neighbour);
								}
								if((boolean) (neighbour.getPhyloLeaf().getUserData().get(MaxLikelihoodPlacer.PLACER_VALID_TARGET_USER_DATA_KEY))) {
									if(closestTarget == null || neighbour.getDistance().compareTo(closestTarget.getDistance()) < 0) {
										closestTarget = neighbour;
									}
								}
								allNeighboursScaledDistanceTotal = allNeighboursScaledDistanceTotal + scaledDistance;
								Double currentTotal = almtNameToScaledDistanceTotal.get(neighbourAncestorAlmtName);
								if(currentTotal == null) {
									currentTotal = 0.0;
								}
								currentTotal = currentTotal + scaledDistance;
								almtNameToScaledDistanceTotal.put(neighbourAncestorAlmtName, currentTotal);
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
					if(closestTarget != null) {
						Map<String,String> closestTargetPkMap = 
								Project.targetPathToPkMap(ConfigurableTable.alignment_member, 
										closestTarget.getPhyloLeaf().getName());
						queryCladeCategoryResult.closestTargetAlignmentName = closestTargetPkMap.get(AlignmentMember.ALIGNMENT_NAME_PATH);
						queryCladeCategoryResult.closestTargetSourceName = closestTargetPkMap.get(AlignmentMember.SOURCE_NAME_PATH);
						queryCladeCategoryResult.closestTargetSequenceID = closestTargetPkMap.get(AlignmentMember.SEQUENCE_ID_PATH);
					}
				}
			}
			resultsComplete++;
			if(resultsComplete % 500 == 0) {
				log(Level.FINEST, "Genotyped "+resultsComplete+" of "+singleQueryResults.size()	+" placer results");
				cmdContext.newObjectContext(); // avoid leaking memory
			}
		}
		cmdContext.newObjectContext(); // avoid leaking memory
		return queryGenotypingResults;
	
	}
	
	@Override
	public List<? extends BaseCladeCategory> getCladeCategories() {
		return cladeCategories;
	}
	
	
}
