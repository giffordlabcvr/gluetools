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
package uk.ac.gla.cvr.gluetools.core.phylotree;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeReconcilerException.Code;

/**
 * PhyloTreeVisitor which vists one phylo tree "visited", and reconciles it 
 * with another phylo tree, supplied in constructor "supplied".
 * 
 * This is done by pushing/popping PhyloObjects from the supplied phylo tree onto a stack,
 * The visitor also checks that the topologies and internal/leaf names match exactly, 
 * throwing an exception if they do not match.
 * Corresponding objects are linked via privately stored maps, which can be obtained after accept(...) completes
 * 
 */
public class PhyloTreeReconciler implements PhyloTreeVisitor {

	private Map<PhyloObject<?>, PhyloObject<?>> visitedToSupplied = new LinkedHashMap<PhyloObject<?>, PhyloObject<?>>();
	private Map<PhyloObject<?>, PhyloObject<?>> suppliedToVisited = new LinkedHashMap<PhyloObject<?>, PhyloObject<?>>();
	
	private LinkedList<PhyloObject<?>> suppliedPhyloObjStack = new LinkedList<PhyloObject<?>>();

	public PhyloTreeReconciler(PhyloTree suppliedPhyloTree) {
		push(suppliedPhyloTree);
	}
	
	@Override
	public void preVisitTree(PhyloTree visitedPhyloTree) {
		PhyloTree suppliedPhyloTree = peek(PhyloTree.class);
		link(suppliedPhyloTree, visitedPhyloTree);
		push(suppliedPhyloTree.getRoot());
	}

	@Override
	public void postVisitTree(PhyloTree visitedPhyloTree) {
		pop(PhyloTree.class);
		if(!suppliedPhyloObjStack.isEmpty()) {
			throw new PhyloTreeReconcilerException(Code.PHYLO_TREE_RECONCILER_ERROR, "Reconciler expected stack to be empty");
		}

	}

	@Override
	public void preVisitInternal(PhyloInternal visitedPhyloInternal) {
		PhyloInternal suppliedPhyloInternal = peek(PhyloInternal.class);
		
		link(suppliedPhyloInternal, visitedPhyloInternal);
		assertEqual(suppliedPhyloInternal.getName(), visitedPhyloInternal.getName());
		
		List<PhyloBranch> suppliedInternalBranches = suppliedPhyloInternal.getBranches();
		if(suppliedInternalBranches.size() != visitedPhyloInternal.getBranches().size()) {
			throw new PhyloTreeReconcilerException(Code.PHYLO_TREE_RECONCILER_ERROR, "Reconciler expected internal nodes to have same number of branches");
		}
		if(!suppliedInternalBranches.isEmpty()) {
			push(suppliedInternalBranches.get(0));
		}
	}

	@Override
	public void postVisitInternal(PhyloInternal visitedPhyloInternal) {
		pop(PhyloInternal.class);
	}

	@Override
	public void preVisitBranch(int branchIndex, PhyloBranch visitedPhyloBranch) {
		PhyloBranch suppliedPhyloBranch = peek(PhyloBranch.class);
		link(suppliedPhyloBranch, visitedPhyloBranch);
		push(suppliedPhyloBranch.getSubtree());
	}


	@Override
	public void postVisitBranch(int branchIndex, PhyloBranch visitedPhyloBranch) {
		pop(PhyloBranch.class);
		PhyloInternal suppliedInternal = peek(PhyloInternal.class);
		int nextBranchIndex = branchIndex+1;
		List<PhyloBranch> suppliedInternalBranches = suppliedInternal.getBranches();
		if(nextBranchIndex < suppliedInternalBranches.size()) {
			push(suppliedInternalBranches.get(nextBranchIndex));
		}
	}
	
	@Override
	public void visitLeaf(PhyloLeaf visitedPhyloLeaf) {
		PhyloLeaf suppliedPhyloLeaf = pop(PhyloLeaf.class);
		assertEqual(suppliedPhyloLeaf.getName(), visitedPhyloLeaf.getName());
		link(suppliedPhyloLeaf, visitedPhyloLeaf);
	}

	
	private <D extends PhyloObject<?>> void link(D suppliedPhyloObj, D visitedPhyloObj) {
		suppliedToVisited.put(suppliedPhyloObj, visitedPhyloObj);
		visitedToSupplied.put(visitedPhyloObj, suppliedPhyloObj);
	}
	
	private <D extends PhyloObject<?>> D pop(Class<D> theClass) {
		if(suppliedPhyloObjStack.isEmpty()) {
			throw new PhyloTreeReconcilerException(Code.PHYLO_TREE_RECONCILER_ERROR, "Reconciler expected PhyloObject "+theClass.getSimpleName()+" but stack was empty");
		}
		PhyloObject<?> popped = suppliedPhyloObjStack.pop();
		if(!theClass.isAssignableFrom(popped.getClass())) {
			throw new PhyloTreeReconcilerException(Code.PHYLO_TREE_RECONCILER_ERROR, "Reconciler expected PhyloObject "+theClass.getSimpleName()+" but found "+popped.getClass().getSimpleName());
		}
		return theClass.cast(popped);
	}

	private <D extends PhyloObject<?>> D peek(Class<D> theClass) {
		if(suppliedPhyloObjStack.isEmpty()) {
			throw new PhyloTreeReconcilerException(Code.PHYLO_TREE_RECONCILER_ERROR, "Reconciler expected PhyloObject "+theClass.getSimpleName()+" but stack was empty");
		}
		PhyloObject<?> peeked = suppliedPhyloObjStack.peek();
		if(!theClass.isAssignableFrom(peeked.getClass())) {
			throw new PhyloTreeReconcilerException(Code.PHYLO_TREE_RECONCILER_ERROR, "Reconciler expected PhyloObject "+theClass.getSimpleName()+" but found "+peeked.getClass().getSimpleName());
		}
		return theClass.cast(peeked);
	}
	
	private void push(PhyloObject<?> phyloObj) {
		suppliedPhyloObjStack.push(phyloObj);
	}
	
	private void assertEqual(Object value1, Object value2) {
		if(value1 == null && value2 == null) {
			return;
		}
		if(value1 == null || value2 == null || !value1.equals(value2)) {
			throw new PhyloTreeReconcilerException(Code.PHYLO_TREE_RECONCILER_ERROR, "Mismatched values: "+value1+" does not equal "+value2);
		}
	}

	public Map<PhyloObject<?>, PhyloObject<?>> getVisitedToSupplied() {
		return visitedToSupplied;
	}

	public Map<PhyloObject<?>, PhyloObject<?>> getSuppliedToVisited() {
		return suppliedToVisited;
	}
}