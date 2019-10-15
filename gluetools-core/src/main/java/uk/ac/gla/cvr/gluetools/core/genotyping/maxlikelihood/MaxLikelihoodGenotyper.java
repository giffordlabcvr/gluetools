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
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
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

	// If <useSingleReference> is true, only the closest reference sequence of a given clade will contribute to that clade's weight total.
	// The default is false for backwards compatibility. However the original design does not make much sense because a 
	// clade with a lot of references defined would be unduly heavily weighted. So a validation warning is generated in this case.
	private boolean useSingleReference;
	
	public MaxLikelihoodGenotyper() {
		super();
		registerModulePluginCmdClass(MaxLikelihoodGenotypeFileCommand.class);
		registerModulePluginCmdClass(MaxLikelihoodGenotypeSequenceCommand.class);
		registerModulePluginCmdClass(MaxLikelihoodGenotypeFastaDocumentCommand.class);
		registerModulePluginCmdClass(MaxLikelihoodGenotypePlacerResultCommand.class);
		registerModulePluginCmdClass(MaxLikelihoodGenotypePlacerResultDocumentCommand.class);
		addSimplePropertyName("useSingleReference");
	}

	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		List<Element> categoryElems = PluginUtils.findConfigElements(configElem, "cladeCategory");
		this.cladeCategories = PluginFactory.createPlugins(pluginConfigContext, MaxLikelihoodCladeCategory.class, categoryElems);
		this.useSingleReference = Optional.ofNullable(PluginUtils.configureBooleanProperty(configElem, "useSingleReference", false)).orElse(false);
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
				
				Map<String, List<PlacementNeighbour>> cladeToClosestNeighbours = new LinkedHashMap<String, List<PlacementNeighbour>>();
				PlacementNeighbour closestTarget = null;

				Map<String, Double> almtNameToScaledDistanceTotal = new LinkedHashMap<String, Double>();
				// loop over all neighbours in order of increasing distance.
				// 
				for(MaxLikelihoodSinglePlacement placement: queryResult.singlePlacement) {
					PhyloLeaf placementLeaf = MaxLikelihoodPlacer
							.addPlacementToPhylogeny(glueProjectPhyloTree, edgeIndexToPhyloBranch, queryResult, placement);
					List<PlacementNeighbour> neighbours = PlacementNeighbourFinder.findNeighbours(placementLeaf, null, null);
					for(PlacementNeighbour neighbour: neighbours) {
						BigDecimal distance = neighbour.getDistance();
						// update closest target to be this neighbour as necessary.
						if((boolean) (neighbour.getPhyloLeaf().getUserData().get(MaxLikelihoodPlacer.PLACER_VALID_TARGET_USER_DATA_KEY))) {
							if(closestTarget == null || distance.compareTo(closestTarget.getDistance()) < 0) {
								closestTarget = neighbour;
							}
						}
						if(distance.compareTo(new BigDecimal(distanceCutoff)) <= 0) {
							Double scaledDistance = Math.pow(distance.doubleValue(), distanceScalingExponent) * placement.likeWeightRatio;
							String neighbourLeafName = neighbour.getPhyloLeaf().getName();
							List<String> neighbourAncestorAlmtNames = leafNameToAncestorAlmtNames.get(neighbourLeafName);
							for(String neighbourAncestorAlmtName: neighbourAncestorAlmtNames) {
								if(cladeCategoryAlmtNameToAlmt.containsKey(neighbourAncestorAlmtName)) {
									List<PlacementNeighbour> cladeClosestNeighbours = cladeToClosestNeighbours.get(neighbourAncestorAlmtName);
									if(cladeClosestNeighbours == null) {
										cladeClosestNeighbours = new ArrayList<PlacementNeighbour>();
										cladeToClosestNeighbours.put(neighbourAncestorAlmtName, cladeClosestNeighbours);
									}
									if(cladeClosestNeighbours.size() == 0 || !useSingleReference) {
										cladeClosestNeighbours.add(neighbour);
										Double currentTotal = almtNameToScaledDistanceTotal.get(neighbourAncestorAlmtName);
										if(useSingleReference) {
											currentTotal = scaledDistance;
										} else {
											if(currentTotal == null) {
												currentTotal = 0.0;
											}
											currentTotal = currentTotal + scaledDistance;
										}
										almtNameToScaledDistanceTotal.put(neighbourAncestorAlmtName, currentTotal);
									}
								}
							}
						}	
					}
					MaxLikelihoodPlacer.removePlacementFromPhylogeny(placementLeaf);
				}
				Double allNeighboursScaledDistanceTotal = 0.0;
				for(Double scaledDistance: almtNameToScaledDistanceTotal.values()) {
					allNeighboursScaledDistanceTotal = allNeighboursScaledDistanceTotal + scaledDistance;
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
						List<PlacementNeighbour> closestNeighboursWithinFinalClade = cladeToClosestNeighbours.get(almtName);
						if(closestNeighboursWithinFinalClade != null && closestNeighboursWithinFinalClade.size() > 0) {
							Map<String,String> closestMemberPkMap = 
									Project.targetPathToPkMap(ConfigurableTable.alignment_member, 
											closestNeighboursWithinFinalClade.get(0).getPhyloLeaf().getName());
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

	@Override
	public void validate(CommandContext cmdContext) {
		super.validate(cmdContext);
		if(!this.useSingleReference) {
			GlueLogger.getGlueLogger().warning("Module '"+getModuleName()+"' of type maxLikelihoodGenotyper: consider setting <useSingleReference> to true, to avoid the bias towards clades with more references defined.");
		}
	}
	
	
}
