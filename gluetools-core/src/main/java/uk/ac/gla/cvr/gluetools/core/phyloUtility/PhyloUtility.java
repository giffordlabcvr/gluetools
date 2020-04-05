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
import java.util.Collection;
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
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloInternal;
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
		registerModulePluginCmdClass(ReadAlignmentPhylogenyCommand.class);
		registerModulePluginCmdClass(ReformatPhylogenyCommand.class);
		registerModulePluginCmdClass(UpdateAncestorBranchesCommand.class);
		registerModulePluginCmdClass(UpdateAncestorSubtreesCommand.class);
		registerModulePluginCmdClass(UpdateLeavesCommand.class);
		registerModulePluginCmdClass(AlignmentPhylogenyListNeighboursCommand.class);
	}
	
	public PhyloTree rerootPhylogeny(PhyloBranch rerootBranch, BigDecimal rerootDistance) {
		return PhyloRerooting.rerootPhylogeny(rerootBranch, rerootDistance);
	}

	public PhyloBranch findOutgroupBranch(CommandContext cmdContext,
			Alignment alignment, Expression outgroupWhereClause,
			Expression exWhereClause, PhyloTree phyloTree) {
		Collection<PhyloLeaf> outgroupLeaves = whereClauseToLeaves(cmdContext, alignment, phyloTree, outgroupWhereClause);

		
		if(exWhereClause != null) {
			Collection<PhyloLeaf> nonOutgroupLeaves = whereClauseToLeaves(cmdContext, alignment, phyloTree, exWhereClause);

			Set<PhyloLeaf> outgroupLeafSet = new LinkedHashSet<PhyloLeaf>(outgroupLeaves);
			if(outgroupLeafSet.isEmpty()) {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, "Leaf set specified by outgroupWhereClause is empty.");
			}
			Set<PhyloLeaf> nonOutgroupLeafSet = new LinkedHashSet<PhyloLeaf>(nonOutgroupLeaves);
			if(nonOutgroupLeafSet.isEmpty()) {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, "Leaf set specified by exWhereClause is empty.");
			}
			Set<PhyloLeaf> overlaps = new LinkedHashSet<PhyloLeaf>();
			outgroupLeafSet.forEach(pl -> {
				if(nonOutgroupLeafSet.contains(pl)) { overlaps.add(pl); }
			} );
			nonOutgroupLeafSet.forEach(pl -> {
				if(outgroupLeafSet.contains(pl)) { overlaps.add(pl); }
			} );
			if(!overlaps.isEmpty()) {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, "Leaf sets specified by outgroupWhereClause and exWhereClause overlap.");
			}
			
			List<PhyloBranch> maximallyDominatingBranches = 
					findMaximallyDominatingBranches(phyloTree, outgroupLeafSet, nonOutgroupLeafSet);
			if(maximallyDominatingBranches.size() == 0) {
				throw new CommandException(Code.COMMAND_FAILED_ERROR, "There were zero maximally dominating branches.");
			}
			PhyloBranch longestBranch = null;
			for(PhyloBranch phyloBranch: maximallyDominatingBranches) {
				if(longestBranch == null || phyloBranch.getLength().compareTo(longestBranch.getLength()) > 0) {
					longestBranch = phyloBranch;
				}
			}
			return longestBranch;
		} else {
			return findStrictlyDominatingBranch(outgroupLeaves);
		}
		
	}

	private Collection<PhyloLeaf> whereClauseToLeaves(CommandContext cmdContext, Alignment alignment,
			PhyloTree phyloTree, Expression whereClause) {
		Expression memberExp = ExpressionFactory.matchExp(AlignmentMember.ALIGNMENT_NAME_PATH, alignment.getName()).andExp(whereClause);
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
		Collection<PhyloLeaf> matchingLeaves = memberPkMapToLeaf.values();
		return matchingLeaves;
	}

	
	private List<PhyloBranch> findMaximallyDominatingBranches(PhyloTree phyloTree,
			Set<PhyloLeaf> outgroupLeafSet, Set<PhyloLeaf> nonOutgroupLeafSet) {
		/**
		 * Algorithm for finding the maximally dominating branch.
		 * Each branch with a parent has 4 values to be calculated:
		 * -- p_yes: number of leaves on the parent side which match outgroup
		 * -- p_no:  number of leaves on the parent side which match nonOutgroup
		 * -- c_yes: number of leaves on the child side which match outgroup
		 * -- c_no:  number of leaves on the child side which match nonOutgroup
		 * 
		 * The algorithm returns the list of branches for which max(p_no + c_yes, p_yes + c_no) is maximised.
		 * -- c_yes / c_no are calculated by a simple depth first traversal.
		 * -- we then do a breadth first traversal:
		 *   -- at each branch, p_yes / p_no is the sum of c_yes / c_no of sibling branches
		 *      plus the p_yes / p_no of the parent branch (if any). 
		 */


		phyloTree.accept(new PhyloTreeVisitor() {
			@Override
			public void postVisitBranch(int branchIndex, PhyloBranch phyloBranch) {
				PhyloSubtree<?> subtree = phyloBranch.getSubtree();
				int c_yes = 0, c_no = 0;
				if(subtree instanceof PhyloLeaf) {
					PhyloLeaf subLeaf = (PhyloLeaf) subtree;
					if(outgroupLeafSet.contains(subLeaf)) {
						c_yes = 1;
					} else if(nonOutgroupLeafSet.contains(subLeaf)) {
						c_no = 1;
					}
				} else {
					PhyloInternal subInternal = (PhyloInternal) subtree;
					for(PhyloBranch subBranch: subInternal.getBranches()) {
						c_yes += (Integer) subBranch.getUserData().get("c_yes");
						c_no += (Integer) subBranch.getUserData().get("c_no");
					}
				}
				phyloBranch.ensureUserData().put("c_yes", c_yes);
				phyloBranch.ensureUserData().put("c_no", c_no);
			}
		});

		phyloTree.accept(new PhyloTreeVisitor() {
			@Override
			public void preVisitBranch(int branchIndex, PhyloBranch phyloBranch) {
				int p_yes = 0, p_no = 0;
				PhyloInternal parentInternal = phyloBranch.getParentPhyloInternal();
				PhyloBranch parentBranch = parentInternal.getParentPhyloBranch();
				if(parentBranch != null) {
					p_yes += (Integer) parentBranch.getUserData().get("p_yes");
					p_no += (Integer) parentBranch.getUserData().get("p_no");
				}
				for(PhyloBranch siblingBranch: parentInternal.getBranches()) {
					if(siblingBranch.getChildBranchIndex() != branchIndex) {
						p_yes += (Integer) siblingBranch.getUserData().get("c_yes");
						p_no += (Integer) siblingBranch.getUserData().get("c_no");
					}
				}
				phyloBranch.getUserData().put("p_yes", p_yes);
				phyloBranch.getUserData().put("p_no", p_no);
			}
		});

		List<PhyloBranch> maximallyDominatingBranches = new ArrayList<PhyloBranch>();
		
		phyloTree.accept(new PhyloTreeVisitor() {
			int bestScore = Integer.MIN_VALUE;
			@Override
			public void postVisitBranch(int branchIndex, PhyloBranch phyloBranch) {
				Map<String, Object> userData = phyloBranch.getUserData();
				int p_yes = (Integer) userData.remove("p_yes");
				int p_no = (Integer) userData.remove("p_no");
				int c_yes = (Integer) userData.remove("c_yes");
				int c_no = (Integer) userData.remove("c_no");
				
				int score = Math.max(p_no + c_yes, p_yes + c_no);
				if(score >= bestScore) {
					if(score > bestScore) {
						bestScore = score;
						maximallyDominatingBranches.clear();
					}
					maximallyDominatingBranches.add(phyloBranch);
				}
			}
		});
		return maximallyDominatingBranches;
	}
	
	private PhyloBranch findStrictlyDominatingBranch(Collection<PhyloLeaf> matchingLeaves) {
		/**
		 * Algorithm for inferring the single dominating branch from the leaf set.
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
		frontier.addAll(matchingLeaves);
		
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
