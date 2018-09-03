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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.SelectQuery;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloSubtree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeVisitor;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;

@PluginClass(elemName="phyloUtility",
		description="Provides various operations on phylogenetic trees")
public class PhyloUtility extends ModulePlugin<PhyloUtility> {

	public PhyloUtility() {
		super();
		registerModulePluginCmdClass(RerootPhylogenyCommand.class);
		registerModulePluginCmdClass(RerootAlignmentPhylogenyCommand.class);
		registerModulePluginCmdClass(ReformatPhylogenyCommand.class);
	}
	
	public PhyloTree rerootPhylogeny(PhyloBranch rerootBranch, BigDecimal rerootDistance) {
		return PhyloRerooting.rerootPhylogeny(rerootBranch, rerootDistance);
	}

	public PhyloBranch findOutgroupBranch(CommandContext cmdContext,
			Alignment alignment, Expression outgroupWhereClause,
			PhyloTree phyloTree) {
		Expression memberExp = ExpressionFactory.matchExp(AlignmentMember.ALIGNMENT_NAME_PATH, alignment.getName()).andExp(outgroupWhereClause);
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
		if(!almtMemberPkMaps.isEmpty()) {
			almtMemberPkMaps.forEach(pkmap -> {
				GlueLogger.getGlueLogger().log(Level.SEVERE, "No leaf found for outgroup alignment member "+pkmap.toString());
			});
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Leaves representing "+almtMemberPkMaps.size()+" outgroup alignment member(s) were not found in the tree. See log for details.");
		}
		
		/**
		 * Algorithm for inferring the single dominating internal node from the leaf set.
		 * Visited means the node has been in the frontier but now is not.
		 * 
		 * 
		 * Add all leaves to the frontier set.
		 * Visited := empty
		 * While true
		 *   If frontier.size == 1, break
		 *   newFrontier := empty
		 *   newVisited := empty
		 *   For each member X of the current frontier
		 *     If X has exactly one neighbour Y not in visited
		 *       Add X to newVisited and add Y to newFrontier
		 *     Else
		 *       Add X to the newFrontier
		 *     End If
		 *   End For
		 *   If newFrontier == frontier, break
		 *   frontier = newFrontier
		 *   add all newVisited to visited
		 * End While
		 * 
		 */
		
		LinkedHashSet<PhyloSubtree<?>> frontier = new LinkedHashSet<PhyloSubtree<?>>();
		frontier.addAll(memberPkMapToLeaf.values());
		
		Set<PhyloSubtree<?>> visited = new LinkedHashSet<PhyloSubtree<?>>();
		
		while(true) {
			//GlueLogger.getGlueLogger().log(Level.FINEST, "Visited size "+visited.size());
			//GlueLogger.getGlueLogger().log(Level.FINEST, "Frontier size "+frontier.size());
			if(frontier.size() == 1) {
				break;
			}
			LinkedHashSet<PhyloSubtree<?>> newFrontier = new LinkedHashSet<PhyloSubtree<?>>();
			List<PhyloSubtree<?>> newVisited = new ArrayList<PhyloSubtree<?>>();
			for(PhyloSubtree<?> subtree: frontier) {
				List<PhyloSubtree<?>> unvisitedNeighbours = unvisitedNeighbours(visited, subtree);
				if(unvisitedNeighbours.size() == 1) {
					newVisited.add(subtree);
					newFrontier.add(unvisitedNeighbours.get(0)); // single unvisited neighbour replaces subtree.
				} else if(unvisitedNeighbours.size() > 1) {
					newFrontier.add(subtree);
				}
			}
			if(newFrontier.equals(frontier)) {
				break;
			}
			frontier = newFrontier;
			visited.addAll(newVisited);
		}
		if(frontier.size() == 0) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Outgroup rerooting impossible as outgroup covers whole tree");
		}
		if(frontier.size() > 1) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Outgroup rerooting impossible as outgroup does not have unique ancestor");
		}
		PhyloSubtree<?> finalNode = frontier.iterator().next();
		// GlueLogger.getGlueLogger().log(Level.FINEST, "Final node bootstraps: "+finalNode.getUserData().get("bootstraps"));
		
		Optional<PhyloBranch> rerootBranch = finalNode.getNeighbourBranches().stream().filter(branch -> !visited.contains(branch.otherSubtree(finalNode))).findFirst();
		if(rerootBranch.isPresent()) {
			return rerootBranch.get();
		} else {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Outgroup rerooting impossible as outgroup covers whole tree");
		}
	}
	
	
	private List<PhyloSubtree<?>> unvisitedNeighbours(Set<PhyloSubtree<?>> visited, PhyloSubtree<?> subtree) {
		return subtree.getNeighbours().stream().filter(n -> !visited.contains(n)).collect(Collectors.toList());
	}
	
}
