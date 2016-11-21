package uk.ac.gla.cvr.gluetools.core.treeRerooter;

import java.math.BigDecimal;
import java.util.LinkedList;

import uk.ac.gla.cvr.gluetools.core.modules.ModulePlugin;
import uk.ac.gla.cvr.gluetools.core.newick.NewickPhyloTreeVisitor;
import uk.ac.gla.cvr.gluetools.core.newick.NewickToPhyloTreeParser;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloInternal;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloSubtree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeVisitor;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;

@PluginClass(elemName="treeRerooter")
public class TreeRerooter extends ModulePlugin<TreeRerooter> {

	public TreeRerooter() {
		super();
		addModulePluginCmdClass(RerootTreeCommand.class);
	}

	public PhyloTree rerootTree(PhyloBranch branchWithRootPoint, BigDecimal rootPointDistance) {
		if(rootPointDistance.compareTo(BigDecimal.valueOf(0.0)) < 0 
				|| rootPointDistance.compareTo(branchWithRootPoint.getLength()) > 0) {
			throw new RuntimeException("Illegal root point distance");
		}
		PhyloTree rerootedTree = branchWithRootPoint.getTree().clone();
		PhyloInternal rerootedInternal = new PhyloInternal();
		rerootedTree.setRoot(rerootedInternal);
		
		LinkedList<RerootTask<?>> taskQueue = new LinkedList<RerootTask<?>>();
		
		PhyloBranch cloneOfBranchToChild = branchWithRootPoint.clone();
		cloneOfBranchToChild.setLength(branchWithRootPoint.getLength().subtract(rootPointDistance));
		rerootedInternal.addBranch(cloneOfBranchToChild);

		PhyloBranch cloneOfBranchToParent = branchWithRootPoint.clone();
		cloneOfBranchToParent.setLength(rootPointDistance);
		rerootedInternal.addBranch(cloneOfBranchToParent);
		
		
		addSubtreeToTaskQueue(branchWithRootPoint.getSubtree(), new FromParentRerootDirection(), 
				cloneOfBranchToChild, taskQueue);
		addSubtreeToTaskQueue(branchWithRootPoint.getParentPhyloInternal(), new FromChildRerootDirection(branchWithRootPoint.getChildBranchIndex()), 
				cloneOfBranchToParent, taskQueue);
		
		while(!taskQueue.isEmpty()) {
			
			RerootTask<?> rerootTask = taskQueue.pop();
			rerootTask.rerootDirection.processTask(rerootTask, taskQueue);
			
		}
		return rerootedTree;
	}

	private abstract static class RerootDirection {
		public abstract <D extends PhyloSubtree<?>> void processTask(RerootTask<D> rerootTask, LinkedList<RerootTask<?>> taskQueue);
	}
	@SuppressWarnings("unchecked")
	private static class FromParentRerootDirection extends RerootDirection {
		@Override
		public <D extends PhyloSubtree<?>> void processTask(RerootTask<D> rerootTask, LinkedList<RerootTask<?>> taskQueue) {
			D clonedSubtree = (D) rerootTask.originalSubtree.clone();
			rerootTask.clonedBranch.setSubtree(clonedSubtree);
			if(clonedSubtree instanceof PhyloInternal) {
				PhyloInternal originalInternal = (PhyloInternal) rerootTask.originalSubtree;
				PhyloInternal clonedInternal = (PhyloInternal) clonedSubtree;
				for(PhyloBranch originalChildBranch : originalInternal.getBranches()) {
					PhyloBranch clonedChildBranch  = originalChildBranch.clone();
					clonedInternal.addBranch(clonedChildBranch);
					addSubtreeToTaskQueue(originalChildBranch.getSubtree(), new FromParentRerootDirection(), clonedChildBranch, taskQueue);
				}
			}
		}
		
	}
	@SuppressWarnings("unchecked")
	private static class FromChildRerootDirection extends RerootDirection {
		private int rerootBranchIndex;
		public FromChildRerootDirection(int rerootBranchIndex) {
			super();
			this.rerootBranchIndex = rerootBranchIndex;
		}
		@Override
		public <D extends PhyloSubtree<?>> void processTask(RerootTask<D> rerootTask, LinkedList<RerootTask<?>> taskQueue) {
			PhyloInternal originalInternal = (PhyloInternal) rerootTask.originalSubtree;
			PhyloBranch originalParentBranch = originalInternal.getParentPhyloBranch();
			if(originalParentBranch == null) {
				if(originalInternal.getBranches().size() == 2) {
					// internal node is at the root of the tree, and will be discarded.
					for(PhyloBranch originalChildBranch: originalInternal.getBranches()) {
						if(originalChildBranch.getChildBranchIndex() != rerootBranchIndex) {
							PhyloBranch clonedBranch = rerootTask.clonedBranch;
							clonedBranch.setLength(clonedBranch.getLength().add(originalChildBranch.getLength()));
							PhyloSubtree<?> originalSubtree = originalChildBranch.getSubtree();
							addSubtreeToTaskQueue(originalSubtree, 
									new FromParentRerootDirection(), clonedBranch, taskQueue);
						}
					}
					
				} else {
					PhyloInternal clonedInternal = (PhyloInternal) originalInternal.clone();
					rerootTask.clonedBranch.setSubtree(clonedInternal);
					for(PhyloBranch originalChildBranch: originalInternal.getBranches()) {
						if(originalChildBranch.getChildBranchIndex() != rerootBranchIndex) {
							PhyloBranch clonedChildBranch  = originalChildBranch.clone();
							clonedInternal.addBranch(clonedChildBranch);
							addSubtreeToTaskQueue(originalChildBranch.getSubtree(), 
									new FromParentRerootDirection(), clonedChildBranch, taskQueue);
						}
					}
				}
				
				
			} else {
				// polytomy at root
				PhyloInternal clonedInternal = (PhyloInternal) originalInternal.clone();
				rerootTask.clonedBranch.setSubtree(clonedInternal);
				for(PhyloBranch originalChildBranch: originalInternal.getBranches()) {
					if(originalChildBranch.getChildBranchIndex() != rerootBranchIndex) {
						PhyloBranch clonedChildBranch  = originalChildBranch.clone();
						clonedInternal.addBranch(clonedChildBranch);
						addSubtreeToTaskQueue(originalChildBranch.getSubtree(), 
								new FromParentRerootDirection(), clonedChildBranch, taskQueue);
					}
				}
				PhyloBranch clonedParentBranch  = originalParentBranch.clone();
				clonedInternal.addBranch(clonedParentBranch);
				addSubtreeToTaskQueue(originalParentBranch.getParentPhyloInternal(), 
						new FromChildRerootDirection(originalParentBranch.getChildBranchIndex()), 
						clonedParentBranch, taskQueue);
			}
			
		}
	}

	@SuppressWarnings("unchecked")
	private static <D extends PhyloSubtree<?>> void addSubtreeToTaskQueue(D originalSubtree, RerootDirection rerootDirection, 
			PhyloBranch clonedBranch, LinkedList<RerootTask<?>> taskQueue) {
		RerootTask<D> rerootTask = new RerootTask<D>();
		rerootTask.originalSubtree = originalSubtree;
		rerootTask.clonedBranch = clonedBranch;
		rerootTask.rerootDirection = rerootDirection;
		taskQueue.add(rerootTask);
	}
	
	private static class RerootTask<D extends PhyloSubtree<?>> {
		RerootDirection rerootDirection;
		D originalSubtree; 
		PhyloBranch clonedBranch; 
	}
	
	private static class LeafHolder { PhyloLeaf leaf; }
	private static PhyloLeaf findLeaf(PhyloTree tree, String name) {
		LeafHolder leafHolder = new LeafHolder();
		tree.accept(new PhyloTreeVisitor() {
			@Override
			public void visitLeaf(PhyloLeaf phyloLeaf) {
				if(phyloLeaf.getName().equals(name)) {
					leafHolder.leaf = phyloLeaf;
				}
			}
		});
		return leafHolder.leaf;
	}

	private static String treeToString(PhyloTree tree) {
		NewickPhyloTreeVisitor newickPhyloTreeVisitor = new NewickPhyloTreeVisitor();
		tree.accept(newickPhyloTreeVisitor);
		return newickPhyloTreeVisitor.getNewickString();
	}
	private static void check(String expected, String actual) {
		if(expected.equals(actual)) {
			System.out.println("Correct: "+expected);
		} else {
			System.out.println("Test failure, expected: "+expected+", actual: "+actual);
		}
	}
	
	public static void main(String[] args) {
		TreeRerooter treeRerooter = new TreeRerooter();
		NewickToPhyloTreeParser parser = new NewickToPhyloTreeParser();
		/*
		 *              +-2-X-2-C
		 *              |
		 *      +-2-Y-1-+
		 *      |       |
		 *      |       +---2---D
		 *      + 
		 *      |       +-1-Z-4-E
		 *      |       |
		 *      +---2---+
		 *              |
		 *              +---3---G
		 *      
		 */      
		PhyloTree startTree = parser.parseNewick("((C:4,D:2):3,(E:5,G:3):2);");
		/*
		 * Case 1: Rerooting at point X should produce
		 * 
		 * (C:2,(D:2,(E:5,G:3):5):2)
		 */
		PhyloBranch case1RerootBranch = findLeaf(startTree, "C").getParentPhyloBranch();
		PhyloTree case1Tree = treeRerooter.rerootTree(case1RerootBranch, BigDecimal.valueOf(2));
		check("(C:2,(D:2,(E:5,G:3):5):2);", treeToString(case1Tree));
		/*
		 * Case 2: Rerooting at point Y should produce
		 * 
		 * (C:2,(D:2,(E:5,G:3):5):2)
		 */
		PhyloBranch case2RerootBranch = findLeaf(startTree, "C").getParentPhyloBranch()
					.getParentPhyloInternal().getParentPhyloBranch();
		PhyloTree case2Tree = treeRerooter.rerootTree(case2RerootBranch, BigDecimal.valueOf(2));
		check("((C:4,D:2):1,(E:5,G:3):4);", treeToString(case2Tree));
		/*
		 * Case 3: Rerooting at point Z should produce
		 * 
		 * (E:4,(G:3,(C:4,D:2):5):1)
		 */
		PhyloBranch case3RerootBranch = findLeaf(startTree, "E").getParentPhyloBranch();
		PhyloTree case3Tree = treeRerooter.rerootTree(case3RerootBranch, BigDecimal.valueOf(1));
		check("(E:4,(G:3,(C:4,D:2):5):1);", treeToString(case3Tree));
		/*
		 * Case where there is a polytomy at the root.
		 * 
		 *              +---4---C
		 *              |
		 *      +---3---+
		 *      |       |
		 *      |       +---2---D
		 *      | 
		 *      |       +---5---E
		 *      |       |
		 *      +-1-X-1-+
		 *      |       |
		 *      |       +---3---G
		 *      |
		 *      +-------10------F
		 */      
		PhyloTree polytomyStartTree = parser.parseNewick("((C:4,D:2):3,(E:5,G:3):2,F:10);");
		/*
		 * Polytomy case 1: Rerooting at point X should produce
		 * 
		 * ((E:5,G:3):1,((C:4,D:2):3,F:10):1)
		 */
		PhyloBranch polytomyCase1RerootBranch = findLeaf(polytomyStartTree, "E").getParentPhyloBranch()
				.getParentPhyloInternal().getParentPhyloBranch();
		PhyloTree polytomyCase1Tree = treeRerooter.rerootTree(polytomyCase1RerootBranch, BigDecimal.valueOf(1));
		check("((E:5,G:3):1,((C:4,D:2):3,F:10):1);", treeToString(polytomyCase1Tree));
		
	}
	
}
