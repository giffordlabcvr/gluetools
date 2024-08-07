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
package uk.ac.gla.cvr.gluetools.core.phylogenyImporter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.cayenne.exp.Expression;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.command.project.alignment.AlignmentListMemberCommand;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.builder.ConfigurableTable;
import uk.ac.gla.cvr.gluetools.core.datamodel.project.Project;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.Sequence;
import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloInternal;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloSubtree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeVisitor;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;

@PluginClass(elemName="phyloImporter",
		description="Imports a phylogenetic tree to be stored as auxiliary data in alignment nodes")
public class PhyloImporter extends ModulePlugin<PhyloImporter> {

	// key used on PhyloSubtrees to indicate which GLUE alignment(s) (possibly within an alignment tree) the subtree relates to.
	public static final String GLUE_ALIGNMENT_NAMES_USER_DATA_KEY = "glueAlignmentNames";

	public PhyloImporter() {
		super();
		registerModulePluginCmdClass(ImportPhylogenyCommand.class);
	}

	// anyAlignment -- if true, the alignment mentioned in the name of the incoming leaf 
	// is ignored, instead the member source / seqID must specify 
	// a unique member within the selected alignment members. This is useful for importing a phylogeny 
	// the leaf name is re-written to specify this unique member.
	// generated from an unconstrained alignment into the constrained alignment tree.
	// (in this case you would include referenceMember = false in the <whereClause>)
	
	public List<AlignmentPhylogeny> previewImportPhylogeny(CommandContext cmdContext, PhyloTree phyloTree,
			String rootAlmtName, Boolean recursive, Boolean anyAlignment, Optional<Expression> whereClause) {
		Alignment rootAlmt = GlueDataObject.lookup(cmdContext, Alignment.class, Alignment.pkMap(rootAlmtName));
		if(recursive && !rootAlmt.isConstrained()) {
			throw new CommandException(Code.COMMAND_FAILED_ERROR, "Recursive option can only be used for constrained alignments.");
		}
		List<AlignmentMember> almtMembers = 
				AlignmentListMemberCommand.listMembers(cmdContext, rootAlmt, recursive, whereClause);

		Map<Map<String,String>, Map<String,String>> sequencePkMapToMemberPkMap = new LinkedHashMap<Map<String,String>, Map<String,String>>();
		if(anyAlignment) {
			almtMembers.forEach(memb -> {
				Map<String, String> sequencePkMap = memb.getSequence().pkMap();
				if(sequencePkMapToMemberPkMap.containsKey(sequencePkMap)) {
					throw new CommandException(Code.COMMAND_FAILED_ERROR, "Sequence "+sequencePkMap+" is selected as a member of multiple alignments. This prevents --anyAlignment, since incoming leaves will be ambiguous");
				}
				sequencePkMapToMemberPkMap.put(sequencePkMap, memb.pkMap());
			});
		}
		
		// init AlignmentData map:
		Map<String, AlignmentData> alignmentNameToData = new LinkedHashMap<String, AlignmentData>();
		almtMembers.forEach(memb -> {
			// ensure the alignment of each selected member is in the alignment data map.
			Alignment memberAlmt = memb.getAlignment();
			AlignmentData alignmentData = addAlignmentData(alignmentNameToData, memberAlmt, rootAlmt);
			// register the selected member against the alignment data.
			alignmentData.memberPkMaps.add(memb.pkMap());
		});
		
		// for each phylo tree leaf, ensure it maps to a selected member. 
		phyloTree.accept(new PhyloTreeVisitor() {
			@Override
			public void visitLeaf(PhyloLeaf phyloLeaf) {
				String leafNodeName = phyloLeaf.getName();
				Map<String, String> leafNodePkMap = memberLeafNodeNameToPkMap(leafNodeName);
				Map<String, String> selectedMemberPkMap;
				if(anyAlignment) {
					selectedMemberPkMap = sequencePkMapToMemberPkMap.get(
							Sequence.pkMap(leafNodePkMap.get(AlignmentMember.SOURCE_NAME_PATH), 
									leafNodePkMap.get(AlignmentMember.SEQUENCE_ID_PATH)));
					if(selectedMemberPkMap == null) {
						throw new ImportPhylogenyException(ImportPhylogenyException.Code.MEMBER_LEAF_MISMATCH, "Leaf node "+leafNodeName+" does not match any selected alignment member, even allowing --anyAlignment.");
					}
					phyloLeaf.setName(memberPkMapToLeafNodeName(selectedMemberPkMap));
				} else {
					selectedMemberPkMap = leafNodePkMap;
				}
				String alignmentName = selectedMemberPkMap.get(AlignmentMember.ALIGNMENT_NAME_PATH);
				AlignmentData alignmentData = alignmentNameToData.get(alignmentName);
				if(alignmentData == null) {
					throw new ImportPhylogenyException(ImportPhylogenyException.Code.MEMBER_LEAF_MISMATCH, "Leaf node "+leafNodeName+" does not match any selected alignment.");
				}
				if(!alignmentData.memberPkMaps.contains(selectedMemberPkMap)) {
					throw new ImportPhylogenyException(ImportPhylogenyException.Code.MEMBER_LEAF_MISMATCH, "Leaf node "+leafNodeName+" does not match any selected alignment member.");
				}
				// register the leaf against the relevant alignment data.
				alignmentData.memberPkMapToLeaf.put(selectedMemberPkMap, phyloLeaf);
			}

		});

		// check that all selected members map to a phylo leaf node.
		for(AlignmentData almtData : alignmentNameToData.values()) {
			for(Map<String,String> memberPkMap : almtData.memberPkMaps) {
				if(!almtData.memberPkMapToLeaf.containsKey(memberPkMap)) {
					throw new ImportPhylogenyException(ImportPhylogenyException.Code.MEMBER_LEAF_MISMATCH, "Alignment member "+memberPkMap+" does not match any leaf node.");
				}
			}
		}
		
		// sort alignment datas by decreasing depth.
		List<AlignmentData> sortedAlmtDataList = new ArrayList<AlignmentData>(alignmentNameToData.values());
		sortedAlmtDataList.sort(new Comparator<AlignmentData>() {
			@Override
			public int compare(AlignmentData o1, AlignmentData o2) {
				return - Integer.compare(o1.depth, o2.depth);
			}
		});
		// Identify the section of the phylo tree relating to each alignment: the clade subtree
		sortedAlmtDataList.forEach(almtData -> {
			Alignment thisAlmt = almtData.alignment;
			almtData.cladeSubtree = findCladeSubtree(almtData);
			String thisAlmtName = thisAlmt.getName();
			if(!thisAlmtName.equals(rootAlmt.getName())) {
				// register the selected clade subtree against the relevant child of the parent alignment data.
				AlignmentData parentAlignmentData = alignmentNameToData.get(thisAlmt.getParent().getName());
				parentAlignmentData.subtreeToChildAlmtPkMap.put(almtData.cladeSubtree, thisAlmt.pkMap());
			}
		});
		// for each alignment create the new alignment phylogeny.
		return sortedAlmtDataList.stream().map(almtData -> createAlignmentPhylogeny(phyloTree, almtData)).collect(Collectors.toList());
	}
	
	private AlignmentPhylogeny createAlignmentPhylogeny(PhyloTree importedTree, AlignmentData almtData) {
		// log(Level.FINE, "Creating phylogeny for alignment "+almtData.alignment.getName());
		PhyloTree phyloTree = importedTree.clone();
		AlignmentPhylogeny alignmentPhylogeny = new AlignmentPhylogeny();
		alignmentPhylogeny.setAlignment(almtData.alignment);
		alignmentPhylogeny.setPhyloTree(phyloTree);
		phyloTree.setRoot(cloneCladeSubtree(almtData.cladeSubtree, almtData, alignmentPhylogeny));
		String alignmentName = almtData.alignment.getName();
		phyloTree.getRoot().ensureUserData().put(GLUE_ALIGNMENT_NAMES_USER_DATA_KEY, Arrays.asList(alignmentName));
		// log(Level.FINE, "Phylogeny for alignment "+almtData.alignment.getName()+":\n"+
		// new String(PhyloFormat.GLUE_JSON.generate(phyloTree));
		return alignmentPhylogeny;
	}

	private PhyloSubtree<?> cloneCladeSubtree(PhyloSubtree<?> cladeSubtree, AlignmentData almtData, AlignmentPhylogeny alignmentPhylogeny) {
		// log(Level.FINEST, "Cloning subtree "+cladeSubtree.toString());
		Map<String,String> childAlmtPkMap = almtData.subtreeToChildAlmtPkMap.get(cladeSubtree);
		if(childAlmtPkMap != null) {
			PhyloLeaf pointerLeaf = new PhyloLeaf();
			pointerLeaf.setName("alignment/"+childAlmtPkMap.get(Alignment.NAME_PROPERTY));
			alignmentPhylogeny.incrementPointerLeafNodes();
			return pointerLeaf;
		} else {
			if(cladeSubtree instanceof PhyloLeaf) {
				PhyloLeaf clonedLeaf = (PhyloLeaf) cladeSubtree.clone();
				alignmentPhylogeny.incrementMemberLeafNodes();
				return clonedLeaf;
			} else {
				PhyloInternal cladeInternal = (PhyloInternal) cladeSubtree;
				PhyloInternal clonedInternal = cladeInternal.clone();
				alignmentPhylogeny.incrementInternalNodes();
				for(PhyloBranch phyloBranch : cladeInternal.getBranches()) {
					PhyloBranch clonedBranch = phyloBranch.clone();
					clonedInternal.addBranch(clonedBranch);
					clonedBranch.setSubtree(cloneCladeSubtree(phyloBranch.getSubtree(), almtData, alignmentPhylogeny));
				}
				return clonedInternal;
			}
		}
	}

	public static class AlignmentPhylogeny {
		private Alignment alignment;
		private PhyloTree phyloTree;
		private int memberLeafNodes;
		private int pointerLeafNodes;
		private int internalNodes;
		
		public int getMemberLeafNodes() {
			return memberLeafNodes;
		}
		public void incrementMemberLeafNodes() {
			this.memberLeafNodes++;
		}
		public int getPointerLeafNodes() {
			return pointerLeafNodes;
		}
		public void incrementPointerLeafNodes() {
			this.pointerLeafNodes++;
		}
		public int getInternalNodes() {
			return internalNodes;
		}
		public void incrementInternalNodes() {
			this.internalNodes++;
		}
		public void setAlignment(Alignment alignment) {
			this.alignment = alignment;
		}
		public void setPhyloTree(PhyloTree phyloTree) {
			this.phyloTree = phyloTree;
		}
		public Alignment getAlignment() {
			return alignment;
		}
		public PhyloTree getPhyloTree() {
			return phyloTree;
		}
		
		
	}
	
	private PhyloSubtree<?> findCladeSubtree(AlignmentData almtData) {
		// log(Level.FINE, "Finding clade subtree for alignment "+almtData.alignment.getName());
		// initialise the set of required leaf / internal nodes
		// we will remove items from this set as we ecounter ancestor nodes including them
		Set<PhyloSubtree<?>> requiredSubtrees = new LinkedHashSet<PhyloSubtree<?>>(almtData.subtreeToChildAlmtPkMap.keySet());
		requiredSubtrees.addAll(almtData.memberPkMapToLeaf.values());
		// pick an arbitrary starting node within the clade.
		PhyloSubtree<?> nodeWithinClade = requiredSubtrees.iterator().next();
		PhyloSubtree<?> previousSubtree = null;
		PhyloSubtree<?> currentSubtree = nodeWithinClade;
		// walk up the tree from the start point until all requiredSubtrees are accounted for.
		while(!requiredSubtrees.isEmpty()) {
			if(currentSubtree instanceof PhyloLeaf) {
				// log(Level.FINEST, "Found required leaf "+((PhyloLeaf) currentSubtree).getName());
				requiredSubtrees.remove(currentSubtree);
				// log(Level.FINEST, "Remaining subtrees required "+requiredSubtrees.size());
			} else if(requiredSubtrees.remove(currentSubtree)) {
				// log(Level.FINEST, "Found required internal");
				// log(Level.FINEST, "Remaining subtrees required "+requiredSubtrees.size());
			} else if(!processSubtree(previousSubtree, (PhyloInternal) currentSubtree, requiredSubtrees)) {
				// below currentSubtree we found a leaf which is 
				// (a) not in the required set (b) not descended from any required internal node.
				break; 
			}
			if(requiredSubtrees.isEmpty()) {
				break;
			}
			// save previous subtree to make sure we do not visit it on the way back down.
			previousSubtree = currentSubtree;
			PhyloBranch parentPhyloBranch = currentSubtree.getParentPhyloBranch();
			if(parentPhyloBranch != null) {
				currentSubtree = parentPhyloBranch.getParentPhyloInternal();
			} else {
				break; // at root of phylo tree.
			}
		}
		if(!requiredSubtrees.isEmpty()) {
			throw new ImportPhylogenyException(
					ImportPhylogenyException.Code.PHYLOGENY_INCONSISTENT, "No subtree contains correct descendents for "+almtData.alignment.getName());
		}
		if(currentSubtree instanceof PhyloLeaf) {
			// log(Level.FINE, "Clade subtree for alignment "+almtData.alignment.getName()+" was leaf "+
			// ((PhyloLeaf) currentSubtree).getName());
		} else {
			// log(Level.FINE, "Clade subtree for alignment "+almtData.alignment.getName()+" was an internal node");
		}
		return currentSubtree;
	}

	private boolean processSubtree(PhyloSubtree<?> previousSubtree, PhyloInternal currentSubtree, Set<PhyloSubtree<?>> requiredSubtrees) {
		for(PhyloBranch phyloBranch: ((PhyloInternal) currentSubtree).getBranches()) {
			PhyloSubtree<?> childSubtree = phyloBranch.getSubtree();
			if(childSubtree == previousSubtree) {
				continue;
			}
			boolean childSubtreeWasRequired = requiredSubtrees.remove(childSubtree);
			if(childSubtree instanceof PhyloLeaf) {
				if(!childSubtreeWasRequired) {
					// log(Level.FINEST, "Found 'foreign' leaf "+((PhyloLeaf) childSubtree).getName());
					return false; // found a leaf which shouldn't be in this clade.
				}
				// log(Level.FINEST, "Found required leaf "+((PhyloLeaf) childSubtree).getName());
				// log(Level.FINEST, "Remaining subtrees required "+requiredSubtrees.size());
				continue;
			}
			PhyloInternal childPhyloInternal = (PhyloInternal) childSubtree;
			if(childSubtreeWasRequired) {
				// log(Level.FINEST, "Found required internal");
				// log(Level.FINEST, "Remaining subtrees required "+requiredSubtrees.size());
				continue;
			}
			processSubtree(null, childPhyloInternal, requiredSubtrees);
		}
		return true;
	}

	private AlignmentData addAlignmentData(
			Map<String, AlignmentData> alignmentNameToData, Alignment alignment, Alignment rootAlignment) {
		String almtName = alignment.getName();
		AlignmentData alignmentData = alignmentNameToData.get(almtName);
		if(alignmentData == null) {
			alignmentData = new AlignmentData();
			alignmentNameToData.put(almtName, alignmentData);
			alignmentData.alignment = alignment;
			alignmentData.depth = alignment.getDepth();
		}
		if(!alignment.getName().equals(rootAlignment.getName())) {
			// ensure parent alignment is in the alignment data map.
			Alignment parentAlmt = alignment.getParent();
			AlignmentData parentAlmtData = addAlignmentData(alignmentNameToData, parentAlmt, rootAlignment);
			// ensure this alignment's pk map is registered against the parent.
			parentAlmtData.childAlmtPkMaps.add(alignment.pkMap());
		}
		return alignmentData;
	}

	
	public static Map<String, String> memberLeafNodeNameToPkMap(String leafNodeName) {
		return Project.targetPathToPkMap(ConfigurableTable.alignment_member, leafNodeName);
	}

	public static String memberPkMapToLeafNodeName(Map<String, String> pkMap) {
		return Project.pkMapToTargetPath(ConfigurableTable.alignment_member.getModePath(), pkMap);
	}

	private class AlignmentData {
		public PhyloSubtree<?> cladeSubtree;
		Alignment alignment;
		int depth;
		Set<Map<String,String>> memberPkMaps = new LinkedHashSet<Map<String,String>>();
		Set<Map<String,String>> childAlmtPkMaps = new LinkedHashSet<Map<String,String>>();
		Map<Map<String,String>, PhyloLeaf> memberPkMapToLeaf = new LinkedHashMap<Map<String,String>, PhyloLeaf>();
		Map<PhyloSubtree<?>, Map<String,String>> subtreeToChildAlmtPkMap 
			= new LinkedHashMap<PhyloSubtree<?>, Map<String,String>>();
	}
	
	

}
