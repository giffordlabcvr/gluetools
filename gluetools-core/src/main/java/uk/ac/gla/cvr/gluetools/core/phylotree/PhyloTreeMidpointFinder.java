package uk.ac.gla.cvr.gluetools.core.phylotree;

import java.util.LinkedList;
import java.util.List;

import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
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

		Double longestPathLength = longestPathFinder.getLongestPathLength();
		
		GlueLogger.getGlueLogger().finest("Longest path length: "+Double.toString(longestPathLength));
		
		Double midpointPathLength = longestPathLength / 2.0;

		List<PhyloTreeSearchNode> longestPath = longestPathFinder.getLongestPath();
		Double pathLength = 0.0;
		for(PhyloTreeSearchNode searchNode: longestPath) {
			PhyloBranch arrivalBranch = searchNode.getArrivalBranch();
			if(arrivalBranch != null) {
				Double newPathLength = pathLength + arrivalBranch.getLength();
				if(pathLength < midpointPathLength) {
					if(newPathLength >= midpointPathLength) {
						Double rootDistance = null;
						if(searchNode.arrivedFromParent()) {
							// not actually sure this is reachable, given that we started from root.
							rootDistance = midpointPathLength - pathLength;
						} else if(searchNode.arrivedFromChild() != null) {
							rootDistance = newPathLength - midpointPathLength;
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
		private Double longestPathLength = null;
		private Double currentPathLength = 0.0;
		private LinkedList<PhyloTreeSearchNode> searchStack = new LinkedList<PhyloTreeSearchNode>();
		private List<PhyloTreeSearchNode> longestPath = null;

		public void searchFrom(PhyloTreeSearchNode currentNode) {
			searchStack.addLast(currentNode);
			PhyloBranch arrivalBranch = currentNode.getArrivalBranch();
			if(arrivalBranch != null) {
				currentPathLength = currentPathLength + arrivalBranch.getLength();
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
				currentPathLength = currentPathLength - arrivalBranch.getLength();
			}
			searchStack.removeLast();
		}
		public Double getLongestPathLength() {
			return longestPathLength;
		}
		public List<PhyloTreeSearchNode> getLongestPath() {
			return longestPath;
		}
	}
	
	
	private static class FurthestFromRootFinder implements PhyloTreeVisitor {
		private PhyloLeaf furthestFromRoot;
		private LinkedList<PhyloBranch> branchStack = new LinkedList<PhyloBranch>();
		private Double furthestDistanceFromRoot = 0.0;
		private Double distanceFromRoot = 0.0;
		
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
			distanceFromRoot = distanceFromRoot + phyloBranch.getLength();
		}
		@Override
		public void postVisitBranch(int branchIndex, PhyloBranch phyloBranch) {
			branchStack.pop();
			distanceFromRoot = distanceFromRoot + phyloBranch.getLength();
		}
	}
	
	private static PhyloLeaf findLeaf(PhyloTree tree, String name) {
		PhyloLeafFinder phyloLeafFinder = new PhyloLeafFinder(l -> l.getName().equals(name));
		tree.accept(phyloLeafFinder);
		return phyloLeafFinder.getPhyloLeaf();
	}

	private static void test(String testName, PhyloTree tree, PhyloBranch expectedBranch, double rootDistance) {
		PhyloTreeMidpointFinder midpointFinder = new PhyloTreeMidpointFinder();
		PhyloTreeMidpointResult result = midpointFinder.findMidPoint(tree);
		if(result.getBranch() != expectedBranch) {
			System.out.println(testName+": incorrect midpoint branch");
			return;
		} 
		if(result.getRootDistance().doubleValue() != rootDistance) {
			System.out.println(testName+": incorrect rootDistance");
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
		 *      +---3---+
		 *      |       |
		 *      |       +---2---D
		 *      + 
		 *      |       +---5---E
		 *      |       |
		 *      +---2---+
		 *              |
		 *              +---3---G
		 *      
		 */      
		PhyloTree tree1 = parser.parseNewick("((C:5,D:2):3,(E:5,G:3):2);");
		PhyloBranch midpointBranch1 = 
				findLeaf(tree1, "C").getParentPhyloBranch().getParentPhyloInternal().getParentPhyloBranch();
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
				findLeaf(tree2, "E").getParentPhyloBranch().getParentPhyloInternal().getParentPhyloBranch();
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
				findLeaf(tree3, "E").getParentPhyloBranch();
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
				findLeaf(tree4, "G").getParentPhyloBranch();
		test("tree4", tree4, midpointBranch4, 2.5);
	}

	
	
}
