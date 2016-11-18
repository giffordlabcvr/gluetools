package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import java.util.LinkedList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloInternal;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloObject;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeVisitor;
import uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood.MaxLikelihoodPlacerException.Code;

/**
 * PhyloTreeVisitor which vists the JPlace reference phylo tree, and reconciles it 
 * with the phylo tree generated from the GLUE alignment (supplied in constructor).
 * 
 * This is done by pushing/popping PhyloObjects from the GLUE alignment phylo tree onto a stack,
 * and linking corresponding objects via the user data map.
 * 
 * The visitor also checks that the topologies and internal/leaf names match exactly.
 */
public class PhyloTreeReconciler implements PhyloTreeVisitor {
	// User data key mapping glue alignment phylo obj to JPlace phylo obj
	public static final String JPLACE_PHYLO_OBJ = "jPlacePhyloObj";

	// User data key mapping JPlace phylo obj to glue alignment phylo obj
	public static final String GLUE_ALMT_PHYLO_OBJ = "glueAlmtPhyloObj";

	private LinkedList<PhyloObject<?>> glueAlmtPhyloObjStack = new LinkedList<PhyloObject<?>>();

	public PhyloTreeReconciler(PhyloTree glueAlmtPhyloTree) {
		push(glueAlmtPhyloTree);
	}
	
	@Override
	public void preVisitTree(PhyloTree jPlacePhyloTree) {
		PhyloTree glueAlmtPhyloTree = peek(PhyloTree.class);
		link(glueAlmtPhyloTree, jPlacePhyloTree);
		push(glueAlmtPhyloTree.getRoot());
	}

	@Override
	public void postVisitTree(PhyloTree jPlacePhyloTree) {
		pop(PhyloTree.class);
		if(!glueAlmtPhyloObjStack.isEmpty()) {
			throw new MaxLikelihoodPlacerException(Code.PHYLO_TREE_RECONCILER_ERROR, "Reconciler expected stack to be empty");
		}

	}

	@Override
	public void preVisitInternal(PhyloInternal jPlacePhyloInternal) {
		PhyloInternal glueAlmtPhyloInternal = peek(PhyloInternal.class);
		
		link(glueAlmtPhyloInternal, jPlacePhyloInternal);
		assertEqual(glueAlmtPhyloInternal.getName(), jPlacePhyloInternal.getName());
		
		List<PhyloBranch> glueAlmtInternalBranches = glueAlmtPhyloInternal.getBranches();
		if(glueAlmtInternalBranches.size() != jPlacePhyloInternal.getBranches().size()) {
			throw new MaxLikelihoodPlacerException(Code.PHYLO_TREE_RECONCILER_ERROR, "Reconciler expected internal nodes to have same number of branches");
		}
		if(!glueAlmtInternalBranches.isEmpty()) {
			push(glueAlmtInternalBranches.get(0));
		}
	}

	@Override
	public void postVisitInternal(PhyloInternal jPlacePhyloInternal) {
		pop(PhyloInternal.class);
	}

	@Override
	public void preVisitBranch(int branchIndex, PhyloBranch jPlacePhyloBranch) {
		PhyloBranch glueAlmtPhyloBranch = peek(PhyloBranch.class);
		link(glueAlmtPhyloBranch, jPlacePhyloBranch);
		push(glueAlmtPhyloBranch.getSubtree());
	}


	@Override
	public void postVisitBranch(int branchIndex, PhyloBranch jPlacePhyloBranch) {
		pop(PhyloBranch.class);
		PhyloInternal glueAlmtInternal = peek(PhyloInternal.class);
		int nextBranchIndex = branchIndex+1;
		List<PhyloBranch> glueAlmtInternalBranches = glueAlmtInternal.getBranches();
		if(nextBranchIndex < glueAlmtInternalBranches.size()) {
			push(glueAlmtInternalBranches.get(nextBranchIndex));
		}
	}
	
	@Override
	public void visitLeaf(PhyloLeaf jPlacePhyloLeaf) {
		PhyloLeaf glueAlmtPhyloLeaf = pop(PhyloLeaf.class);
		assertEqual(glueAlmtPhyloLeaf.getName(), jPlacePhyloLeaf.getName());
		link(glueAlmtPhyloLeaf, jPlacePhyloLeaf);
	}

	
	private <D extends PhyloObject<?>> void link(D glueAlmtPhyloObj, D jPlacePhyloObj) {
		glueAlmtPhyloObj.ensureUserData().put(JPLACE_PHYLO_OBJ, jPlacePhyloObj);
		jPlacePhyloObj.ensureUserData().put(GLUE_ALMT_PHYLO_OBJ, glueAlmtPhyloObj);
	}
	
	private <D extends PhyloObject<?>> D pop(Class<D> theClass) {
		if(glueAlmtPhyloObjStack.isEmpty()) {
			throw new MaxLikelihoodPlacerException(Code.PHYLO_TREE_RECONCILER_ERROR, "Reconciler expected PhyloObject "+theClass.getSimpleName()+" but stack was empty");
		}
		PhyloObject<?> popped = glueAlmtPhyloObjStack.pop();
		if(!theClass.isAssignableFrom(popped.getClass())) {
			throw new MaxLikelihoodPlacerException(Code.PHYLO_TREE_RECONCILER_ERROR, "Reconciler expected PhyloObject "+theClass.getSimpleName()+" but found "+popped.getClass().getSimpleName());
		}
		return theClass.cast(popped);
	}

	private <D extends PhyloObject<?>> D peek(Class<D> theClass) {
		if(glueAlmtPhyloObjStack.isEmpty()) {
			throw new MaxLikelihoodPlacerException(Code.PHYLO_TREE_RECONCILER_ERROR, "Reconciler expected PhyloObject "+theClass.getSimpleName()+" but stack was empty");
		}
		PhyloObject<?> peeked = glueAlmtPhyloObjStack.peek();
		if(!theClass.isAssignableFrom(peeked.getClass())) {
			throw new MaxLikelihoodPlacerException(Code.PHYLO_TREE_RECONCILER_ERROR, "Reconciler expected PhyloObject "+theClass.getSimpleName()+" but found "+peeked.getClass().getSimpleName());
		}
		return theClass.cast(peeked);
	}
	
	private void push(PhyloObject<?> phyloObj) {
		glueAlmtPhyloObjStack.push(phyloObj);
	}
	
	private void assertEqual(Object value1, Object value2) {
		if(value1 == null && value2 == null) {
			return;
		}
		if(value1 == null || value2 == null || !value1.equals(value2)) {
			throw new MaxLikelihoodPlacerException(Code.PHYLO_TREE_RECONCILER_ERROR, "Mismatched values: "+value1+" does not equal "+value2);
		}
	}
}