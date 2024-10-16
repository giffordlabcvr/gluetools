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

import java.math.BigDecimal;
import java.util.LinkedList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.newick.NewickToPhyloTreeParser;


public class PhyloTreeMidpointFinder {

	public PhyloTreeMidpointResult findMidPoint(PhyloTree phyloTree) {
		FurthestFromRootFinder furthestFromRootFinder = new FurthestFromRootFinder();
		phyloTree.accept(furthestFromRootFinder);
		PhyloLeaf furthestFromRoot = furthestFromRootFinder.getFurthestFromRoot();
		if(furthestFromRoot == null) {
			throw new RuntimeException("No furthestFromRoot found");
		}
		PhyloTreeSearchNode startNode = new PhyloTreeSearchNode(furthestFromRoot);
		
		LongestPathFinder longestPathFinder = new LongestPathFinder();
		longestPathFinder.searchFrom(startNode);

		BigDecimal longestPathLength = longestPathFinder.getLongestPathLength();
		
		BigDecimal midpointPathLength = longestPathLength.divide(new BigDecimal(2.0));

		List<PhyloTreeSearchNode> longestPath = longestPathFinder.getLongestPath();
		BigDecimal pathLength = new BigDecimal(0.0);
		for(PhyloTreeSearchNode searchNode: longestPath) {
			PhyloBranch arrivalBranch = searchNode.getArrivalBranch();
			if(arrivalBranch != null) {
				BigDecimal newPathLength = pathLength.add(arrivalBranch.getLength());
				if(pathLength.compareTo(midpointPathLength) < 0) {
					if(newPathLength.compareTo(midpointPathLength) >= 0) {
						BigDecimal rootDistance = null;
						if(searchNode.arrivedFromParent()) {
							// not actually sure this is reachable, given that we started from root.
							rootDistance = midpointPathLength.subtract(pathLength);
						} else if(searchNode.arrivedFromChild() != null) {
							rootDistance = newPathLength.subtract(midpointPathLength);
						} else {
							throw new RuntimeException("Expected arrivedFromParent or arrivedFromChild");
						}
						return new PhyloTreeMidpointResult(arrivalBranch, rootDistance);
					}
				}
				pathLength = newPathLength;
			}
		}
	throw new RuntimeException("No midpoint found");
	}

	private static class LongestPathFinder {
		private BigDecimal longestPathLength = null;
		private BigDecimal currentPathLength = new BigDecimal(0.0);
		private LinkedList<PhyloTreeSearchNode> searchStack = new LinkedList<PhyloTreeSearchNode>();
		private List<PhyloTreeSearchNode> longestPath = null;

		public void searchFrom(PhyloTreeSearchNode currentNode) {
			searchStack.addLast(currentNode);
			PhyloBranch arrivalBranch = currentNode.getArrivalBranch();
			if(arrivalBranch != null) {
				currentPathLength = currentPathLength.add(arrivalBranch.getLength());
			}
			if(currentNode.getPhyloSubtree() instanceof PhyloLeaf) {
				if(longestPathLength == null || currentPathLength.compareTo(longestPathLength) > 0) {
					longestPathLength = currentPathLength;
					longestPath = new LinkedList<PhyloTreeSearchNode>(searchStack);
				}
			}
			for(PhyloTreeSearchNode nextNode: currentNode.neighbours()) {
				searchFrom(nextNode);
			}
			if(arrivalBranch != null) {
				currentPathLength = currentPathLength.subtract(arrivalBranch.getLength());
			}
			searchStack.removeLast();
		}
		public BigDecimal getLongestPathLength() {
			return longestPathLength;
		}
		public List<PhyloTreeSearchNode> getLongestPath() {
			return longestPath;
		}
	}
	
	
	private static class FurthestFromRootFinder implements PhyloTreeVisitor {
		private PhyloLeaf furthestFromRoot;
		private LinkedList<PhyloBranch> branchStack = new LinkedList<PhyloBranch>();
		private BigDecimal furthestDistanceFromRoot = new BigDecimal(0.0);
		private BigDecimal distanceFromRoot = new BigDecimal(0.0);
		
		public PhyloLeaf getFurthestFromRoot() {
			return furthestFromRoot;
		}
		@Override
		public void visitLeaf(PhyloLeaf phyloLeaf) {
			if(distanceFromRoot.compareTo(furthestDistanceFromRoot) > 0) {
				furthestDistanceFromRoot = distanceFromRoot;
				furthestFromRoot = phyloLeaf;
			}
		}
		@Override
		public void preVisitBranch(int branchIndex, PhyloBranch phyloBranch) {
			branchStack.push(phyloBranch);
			distanceFromRoot = distanceFromRoot.add(phyloBranch.getLength());
		}
		@Override
		public void postVisitBranch(int branchIndex, PhyloBranch phyloBranch) {
			branchStack.pop();
			distanceFromRoot = distanceFromRoot.subtract(phyloBranch.getLength());
		}
	}
	
	private static PhyloSubtree<?> findSubtree(PhyloTree tree, String name) {
		PhyloSubtreeFinder phyloSubtreeFinder = new PhyloSubtreeFinder(l -> name.equals(l.getName()));
		tree.accept(phyloSubtreeFinder);
		return phyloSubtreeFinder.getPhyloSubtree();
	}

	private static void test(String testName, PhyloTree tree, PhyloBranch expectedBranch, double rootDistance) {
		PhyloTreeMidpointFinder midpointFinder = new PhyloTreeMidpointFinder();
		PhyloTreeMidpointResult result = midpointFinder.findMidPoint(tree);
		if(result.getBranch() != expectedBranch) {
			System.out.println(testName+": incorrect midpoint branch");
			return;
		} 
		double actualRootDistance = result.getRootDistance().doubleValue();
		if(actualRootDistance != rootDistance) {
			System.out.println(testName+": incorrect rootDistance: expected "+rootDistance+", actual "+actualRootDistance);
			return;
		} 
		System.out.println("Correct: "+testName);
	}
	
	public static void main(String[] args) {
		NewickToPhyloTreeParser parser = new NewickToPhyloTreeParser();
		/*
		 * tree1
		 *              +---5---C
		 *              |
		 *      +---3---B
		 *      |       |
		 *      |       +---2---D
		 *      A 
		 *      |       +---5---E
		 *      |       |
		 *      +---2---F
		 *              |
		 *              +---3---G
		 *      
		 */      
		PhyloTree tree1 = parser.parseNewick("((C:5,D:2)B:3,(E:5,G:3)F:2)A;");
		PhyloBranch midpointBranch1 = 
				findSubtree(tree1, "B").getParentPhyloBranch();
		test("tree1", tree1, midpointBranch1, 0.5);
		/*
		 * tree2
		 *              +---5---C
		 *              |
		 *      +---3---+
		 *      |       |
		 *      |       +---2---D
		 *      + 
		 *      |       +---5---E
		 *      |       |
		 *      +---5---+
		 *              |
		 *              +---3---G
		 *      
		 */      
		PhyloTree tree2 = parser.parseNewick("((C:5,D:2):3,(E:5,G:3):5);");
		PhyloBranch midpointBranch2 = 
				findSubtree(tree2, "E").getParentPhyloBranch().getParentPhyloInternal().getParentPhyloBranch();
		test("tree2", tree2, midpointBranch2, 1.0);
		/*
		 * tree3
		 *              +---5---C
		 *              |
		 *      +---3---+
		 *      |       |
		 *      |       +---2---D
		 *      + 
		 *      |       +---20--E
		 *      |       |
		 *      +---5---+
		 *              |
		 *              +---3---G
		 *      
		 */      
		PhyloTree tree3 = parser.parseNewick("((C:5,D:2):3,(E:20,G:3):5);");
		PhyloBranch midpointBranch3 = 
				findSubtree(tree3, "E").getParentPhyloBranch();
		test("tree3", tree3, midpointBranch3, 3.5);
		/*
		 * tree4
		 *              +---5---C
		 *              |
		 *      +---3---+
		 *      |       |
		 *      |       +---2---D
		 *      + 
		 *      |       +---20--E
		 *      |       |
		 *      +---5---+
		 *              |
		 *              +---25--G
		 *      
		 */      
		PhyloTree tree4 = parser.parseNewick("((C:5,D:2):3,(E:20,G:25):5);");
		PhyloBranch midpointBranch4 = 
				findSubtree(tree4, "G").getParentPhyloBranch();
		test("tree4", tree4, midpointBranch4, 2.5);
	}

	
	
}
