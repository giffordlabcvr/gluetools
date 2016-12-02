package uk.ac.gla.cvr.gluetools.core.placement.maxlikelihood;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloFormat;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloLeaf;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloSubtreeFinder;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTree;
import uk.ac.gla.cvr.gluetools.core.phylotree.PhyloTreeSearchNode;

// best first search from a leaf, returning neighbour leaves in order of decreasing distance.
public class PlacementNeighbourFinder {

	
	public static List<PlacementNeighbour> findNeighbours(PhyloLeaf startLeaf) {
		PriorityQueue<NeighborSearchNode> searchQueue = new PriorityQueue<NeighborSearchNode>(new Comparator<NeighborSearchNode>() {
			@Override
			public int compare(NeighborSearchNode o1, NeighborSearchNode o2) {
				return o1.distanceFromStart.compareTo(o2.distanceFromStart);
			}
		});

		List<PlacementNeighbour> placementNeighbours = new ArrayList<PlacementNeighbour>();
		
		NeighborSearchNode startNode = new NeighborSearchNode(new PhyloTreeSearchNode(startLeaf), new BigDecimal(0.0));
		searchQueue.add(startNode);
		
		while(!searchQueue.isEmpty()) {
			NeighborSearchNode currentNode = searchQueue.poll();

			PhyloTreeSearchNode currentPhyloTreeSearchNode = currentNode.phyloTreeSearchNode;
			
			if(currentNode != startNode && currentPhyloTreeSearchNode.getPhyloSubtree() instanceof PhyloLeaf) {
				placementNeighbours.add(new PlacementNeighbour((PhyloLeaf) currentPhyloTreeSearchNode.getPhyloSubtree(), currentNode.distanceFromStart));
			}

			List<PhyloTreeSearchNode> phyloTreeNeighbours = currentNode.phyloTreeSearchNode.neighbours();
			List<NeighborSearchNode> newSearchNodes = 
					phyloTreeNeighbours.stream().map(phyTreeNode -> 
						new NeighborSearchNode(phyTreeNode, 
								currentNode.distanceFromStart.add(phyTreeNode.getArrivalBranch().getLength())))
						.collect(Collectors.toList());
			searchQueue.addAll(newSearchNodes);
		}
		return placementNeighbours;
	}

	private static class NeighborSearchNode {
		PhyloTreeSearchNode phyloTreeSearchNode;
		BigDecimal distanceFromStart;
		private NeighborSearchNode(PhyloTreeSearchNode phyloTreeSearchNode,
				BigDecimal distanceFromStart) {
			super();
			this.phyloTreeSearchNode = phyloTreeSearchNode;
			this.distanceFromStart = distanceFromStart;
		}
		
	}
	
	private static PhyloLeaf findLeaf(PhyloTree tree, String name) {
		PhyloSubtreeFinder phyloSubtreeFinder = new PhyloSubtreeFinder(l -> l instanceof PhyloLeaf && name.equals(l.getName()));
		tree.accept(phyloSubtreeFinder);
		return (PhyloLeaf) phyloSubtreeFinder.getPhyloSubtree();
	}

	private static void test(String testName, PhyloLeaf startLeaf, String expectedResult) {
		List<PlacementNeighbour> neighbours = PlacementNeighbourFinder.findNeighbours(startLeaf);
		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < neighbours.size(); i++) {
			if(i > 0) {
				buf.append("/");
			}
			buf.append(neighbours.get(i).getPhyloLeaf().getName());
			buf.append(":");
			buf.append(neighbours.get(i).getDistance());
		};
		String actualResult = buf.toString();
		if(actualResult.equals(expectedResult)) {
			System.out.println("Correct: "+testName+", "+actualResult);
		} else {
			System.out.println("Incorrect: "+testName+", expected: "+expectedResult+", actual: "+actualResult);
		}
	}

	public static void main(String[] args) {
		/*
		 * tree1
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
		PhyloTree tree1 = PhyloFormat.NEWICK.parse("((C:5,D:2):3,(E:20,G:25):5);".getBytes());
		test("tree1, neighbours of G", findLeaf(tree1, "G"), "D:35/C:38/E:45");
		test("tree1, neighbours of D", findLeaf(tree1, "D"), "C:7/E:30/G:35");
		
	}

	
}
