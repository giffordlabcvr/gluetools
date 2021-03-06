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
import java.util.LinkedList;

import uk.ac.gla.cvr.gluetools.core.newick.NewickBootstrapsToPhyloTreeParser;
import uk.ac.gla.cvr.gluetools.core.newick.NewickToPhyloTreeParser;
import uk.ac.gla.cvr.gluetools.core.newick.PhyloTreeToNewickBootstrapsGenerator;
import uk.ac.gla.cvr.gluetools.core.newick.PhyloTreeToNewickGenerator;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloBranch;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloInternal;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloSubtree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloSubtreeFinder;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;

public class PhyloRerooting {

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

	private abstract static class RerootDirection {
		public abstract <D extends PhyloSubtree<?>> void processTask(RerootTask<D> rerootTask, LinkedList<RerootTask<?>> taskQueue);
	}

	private static class RerootTask<D extends PhyloSubtree<?>> {
		RerootDirection rerootDirection;
		D originalSubtree; 
		PhyloBranch clonedBranch; 
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

	private static void check(String expected, String actual) {
		if(expected.equals(actual)) {
			System.out.println("Correct: "+expected);
		} else {
			System.out.println("Test failure, expected: "+expected+", actual: "+actual);
		}
	}

	private static PhyloSubtree<?> findSubtree(PhyloTree tree, String name) {
		PhyloSubtreeFinder phyloSubtreeFinder = new PhyloSubtreeFinder(l -> name.equals(l.getName()));
		tree.accept(phyloSubtreeFinder);
		return phyloSubtreeFinder.getPhyloSubtree();
	}

	public static void main(String[] args) {
		PhyloUtility treeRerooter = new PhyloUtility();
		NewickToPhyloTreeParser parser = new NewickToPhyloTreeParser();
		/*
		 *              +-2-X-2-C
		 *              |
		 *      +-2-Y-1-B
		 *      |       |
		 *      |       +---2---D
		 *      A 
		 *      |       +-1-Z-4-E
		 *      |       |
		 *      +---2---F
		 *              |
		 *              +---3---G
		 *      
		 */      
		PhyloTree startTree = parser.parseNewick("((C:4,D:2)B:3,(E:5,G:3)F:2)A;");
		/*
		 * Case 1: Rerooting at point X should produce
		 * 
		 * (C:2,(D:2,(E:5,G:3)F:5)B:2);
		 */
		PhyloBranch case1RerootBranch = findSubtree(startTree, "C").getParentPhyloBranch();
		PhyloTree case1Tree = treeRerooter.rerootPhylogeny(case1RerootBranch, new BigDecimal(2.0));
		check("(C:2,(D:2,(E:5,G:3)F:5)B:2);", treeToString_NEWICK(case1Tree));
		/* 
		 * Case 2: Rerooting at point Y should produce
		 * 
		 * ((C:4,D:2)B:1,(E:5,G:3)F:4);
		 */
		PhyloBranch case2RerootBranch = findSubtree(startTree, "C").getParentPhyloBranch()
					.getParentPhyloInternal().getParentPhyloBranch();
		PhyloTree case2Tree = treeRerooter.rerootPhylogeny(case2RerootBranch, new BigDecimal(2.0));
		check("((C:4,D:2)B:1,(E:5,G:3)F:4);", treeToString_NEWICK(case2Tree));
		/*
		 * Case 3: Rerooting at point Z should produce
		 * 
		 * (E:4,(G:3,(C:4,D:2)B:5)F:1);
		 */
		PhyloBranch case3RerootBranch = findSubtree(startTree, "E").getParentPhyloBranch();
		PhyloTree case3Tree = treeRerooter.rerootPhylogeny(case3RerootBranch, new BigDecimal(1.0));
		check("(E:4,(G:3,(C:4,D:2)B:5)F:1);", treeToString_NEWICK(case3Tree));
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
		PhyloBranch polytomyCase1RerootBranch = findSubtree(polytomyStartTree, "E").getParentPhyloBranch()
				.getParentPhyloInternal().getParentPhyloBranch();
		PhyloTree polytomyCase1Tree = treeRerooter.rerootPhylogeny(polytomyCase1RerootBranch, new BigDecimal(1.0));
		check("((E:5,G:3):1,((C:4,D:2):3,F:10):1);", treeToString_NEWICK(polytomyCase1Tree));
		
		
		
		
		/*
		 * Test case adapted from Czech et al. 2017, to test whether bootstraps (represented as internal node names)
		 * are in the correct locations after rerooting. Adapted with unit branch lengths implicit in the diagram.
		 * 
		 *      +---------------E
		 *      |        
		 *      |       +---Y---X
		 *      +---2---3
		 *      |   |   +-------B
		 *      |   |    
		 *      |   +-----------A
		 *      |
		 *      |        +------D
		 *      +--------1
		 *               +------C
		 */      
		NewickBootstrapsToPhyloTreeParser bootstrapsParser = new NewickBootstrapsToPhyloTreeParser();
		PhyloTree czech2017StartTree = bootstrapsParser.parseNewick("((C:1,D:1)1:1,(A:1,(B:1,X:1)3:1)2:1,E:1);");
		/*
		 * Rerooting at point Y should produce
		 * 
		 * (X:0.5,(B:1,(A:1,((C:1,D:1)1:1,E:1)2:1)3:1):0.5);
		 */
		PhyloBranch czech2017RerootBranch = findSubtree(czech2017StartTree, "X").getParentPhyloBranch();
		PhyloTree czech2017RerootedTree = treeRerooter.rerootPhylogeny(czech2017RerootBranch, new BigDecimal(0.5));
		check("(X:0.5,(B:1,(A:1,((C:1,D:1)1:1,E:1)2:1)3:1):0.5);", treeToString_NEWICK_BOOTSTRAPS(czech2017RerootedTree));

		
		
		
		
	}

	public static PhyloTree rerootPhylogeny(PhyloBranch branchWithRootPoint, BigDecimal rootPointDistance) {
		if(rootPointDistance.compareTo(new BigDecimal(0.0)) < 0
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

	private static String treeToString_NEWICK(PhyloTree tree) {
		PhyloTreeToNewickGenerator newickPhyloTreeVisitor = new PhyloTreeToNewickGenerator();
		tree.accept(newickPhyloTreeVisitor);
		return newickPhyloTreeVisitor.getNewickString();
	}

	private static String treeToString_NEWICK_BOOTSTRAPS(PhyloTree tree) {
		PhyloTreeToNewickBootstrapsGenerator newickBootstrapsPhyloTreeVisitor = new PhyloTreeToNewickBootstrapsGenerator();
		tree.accept(newickBootstrapsPhyloTreeVisitor);
		return newickBootstrapsPhyloTreeVisitor.getNewickString();
	}

}
