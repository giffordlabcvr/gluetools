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
public class PhyloNeighbourFinder {

	public static List<PhyloNeighbour> findNeighbours(PhyloLeaf startLeaf) {
		return findNeighbours(startLeaf, null, null);
	}
	
	public static List<PhyloNeighbour> findNeighbours(PhyloLeaf startLeaf, BigDecimal distanceCutoff, Integer maxNeighbours) {
		PriorityQueue<NeighborSearchNode> searchQueue = new PriorityQueue<NeighborSearchNode>(new Comparator<NeighborSearchNode>() {
			@Override
			public int compare(NeighborSearchNode o1, NeighborSearchNode o2) {
				return o1.distanceFromStart.compareTo(o2.distanceFromStart);
			}
		});

		List<PhyloNeighbour> placementNeighbours = new ArrayList<PhyloNeighbour>();
		
		NeighborSearchNode startNode = new NeighborSearchNode(new PhyloTreeSearchNode(startLeaf), new BigDecimal(0.0));
		searchQueue.add(startNode);
		
		while(!searchQueue.isEmpty()) {
			NeighborSearchNode currentNode = searchQueue.poll();

			PhyloTreeSearchNode currentPhyloTreeSearchNode = currentNode.phyloTreeSearchNode;
			
			if(currentNode != startNode && currentPhyloTreeSearchNode.getPhyloSubtree() instanceof PhyloLeaf) {
				placementNeighbours.add(new PhyloNeighbour((PhyloLeaf) currentPhyloTreeSearchNode.getPhyloSubtree(),
						placementNeighbours.size()+1,
						currentNode.distanceFromStart));
				if(maxNeighbours != null && maxNeighbours.equals(placementNeighbours.size())) {
					break;
				}
			}

			List<PhyloTreeSearchNode> phyloTreeNeighbours = currentNode.phyloTreeSearchNode.neighbours();
			List<NeighborSearchNode> newSearchNodes = 
					phyloTreeNeighbours.stream().map(phyTreeNode -> 
						new NeighborSearchNode(phyTreeNode, 
								currentNode.distanceFromStart.add(phyTreeNode.getArrivalBranch().getLength())))
						.filter(nsn -> distanceCutoff == null || nsn.distanceFromStart.compareTo(distanceCutoff) <= 0)
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
		List<PhyloNeighbour> neighbours = PhyloNeighbourFinder.findNeighbours(startLeaf);
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
