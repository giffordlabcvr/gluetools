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
package uk.ac.gla.cvr.gluetools.core.segments;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;


public class ReferenceSegmentTree<S extends IReferenceSegment> {

	private Node root = null;
	
	// used to compare segments *only* when refStart is equal.
	private Comparator<S> comparator;
	
	
	public ReferenceSegmentTree(Comparator<S> comparator) {
		this.comparator = comparator;
	}
	
	public ReferenceSegmentTree() {
		this(new Comparator<S>() {
			@Override
			public int compare(S seg1, S seg2) {
				int comp = Integer.compare(seg1.getRefEnd(), seg2.getRefEnd());
				if(comp == 0) {
					comp = Integer.compare(seg1.hashCode(), seg2.hashCode());
				}
				return comp;
			}
		});
	}
	
	private int compareSegs(S seg1, S seg2) {
		int comp = Integer.compare(seg1.getRefStart(), seg2.getRefStart());
		if(comp == 0) {
			comp = comparator.compare(seg1, seg2);
		}
		return comp;
	}
	
	public void checkInvariants() {
		if(root != null) {
			root.checkInvariants();
		}
	}
	
	private class Node {
		S segment;
		Node leftChild = null;
		Node rightChild = null;
		// maximum refEnd value of any descendent node's segment
		int maxRefEnd;

		public Node(S segment) {
			this.segment = segment;
			this.maxRefEnd = segment.getRefEnd();
		}
		
		public String toString() {
			return "( seg="+segment+", maxRefEnd="+maxRefEnd+", left="+leftChild+", right="+rightChild+" )";
		}
		
		public void checkInvariants() {
			if(leftChild != null) {
				if(maxRefEnd < leftChild.maxRefEnd) {
					throw new RuntimeException("Max less than left child max for "+toString());
				}
				if(compareSegs(leftChild.segment, segment) >= 0) {
					throw new RuntimeException("Left child ordering incorrect for "+toString());
				}
			}
			if(rightChild != null) {
				if(maxRefEnd < rightChild.maxRefEnd) {
					throw new RuntimeException("Max less than right child max for "+toString());
				}
				if(compareSegs(segment, rightChild.segment) >= 0) {
					throw new RuntimeException("Left right ordering incorrect for "+toString());
				}
			}
			if(maxRefEnd < segment.getRefEnd()) {
				throw new RuntimeException("Max less than segment end for "+toString());
			}
		}
		
	}
	
	// returns true if node was added.
	public boolean add(S segToAdd) {
		Node addResult = addAux(root, segToAdd);
		if(addResult != null) {
			root = addResult;
			return true;
		}
		return false;
	}
	
	// if segment was added, return new parent node.
	// otherwise return null.
	private Node addAux(Node parent, S segToAdd) {
		if(parent == null) {
			return new Node(segToAdd);
		}
		int comp = compareSegs(segToAdd, parent.segment);
		if(comp < 0) {
			parent.leftChild = addAux(parent.leftChild, segToAdd);
			if(parent.leftChild != null) {
				parent.maxRefEnd = Math.max(parent.maxRefEnd, parent.leftChild.maxRefEnd);
			}
			return parent;
		} else if(comp > 0) {
			parent.rightChild = addAux(parent.rightChild, segToAdd);
			if(parent.rightChild != null) {
				parent.maxRefEnd = Math.max(parent.maxRefEnd, parent.rightChild.maxRefEnd);
			}
			return parent;
		} else {
			return null;
		}
	}

	public void findOverlapping(int refStart, int refEnd, List<S> results) {
		findOverlappingAux(root, refStart, refEnd, results);
	}
	
	private void findOverlappingAux(Node node, int refStart, int refEnd, List<S> results) {
		if(node == null) {
			return;
		}
		if(refStart > node.maxRefEnd) {
			return;
		}
		findOverlappingAux(node.leftChild, refStart, refEnd, results);
		if(node.segment.overlaps(refStart, refEnd)) {
			results.add(node.segment);
		}
		if(refEnd >= node.segment.getRefStart()) {
			findOverlappingAux(node.rightChild, refStart, refEnd, results);
		}
		
	}

	public String toString() {
		if(root == null) {
			return null;
		} else {
			return root.toString();
		}
	}
	
	public static void main(String[] args) {
		List<TestSegment> list = new ArrayList<TestSegment>();
		ReferenceSegmentTree<TestSegment> tree = new ReferenceSegmentTree<TestSegment>();
		
		Random random = new Random(234);
		
		
		for(int i = 0; i < 10000; i++) {
			int segStart = random.nextInt(1000)+1;
			int segEnd = segStart+random.nextInt(50);
			TestSegment testSeg = testSeg(segStart, segEnd);
			list.add(testSeg);
			tree.add(testSeg);
		}

		tree.checkInvariants();
		
		Comparator<TestSegment> comparator = new Comparator<TestSegment>() {
			@Override
			public int compare(TestSegment seg1, TestSegment seg2) {
				int comp = Integer.compare(seg1.getRefStart(), seg2.getRefStart());
				if(comp == 0) {
					comp = Integer.compare(seg1.getRefEnd(), seg2.getRefEnd());
				}
				if(comp == 0) {
					comp = Integer.compare(seg1.hashCode(), seg2.hashCode());
				}
				return comp;
			}
		};
		
		int tests = 50000;
		for(int i = 0; i < tests; i++) {
			int queryStart = random.nextInt(1000)+1;
			int queryEnd = queryStart+random.nextInt(50);

			
			List<TestSegment> listResult = list.stream().filter(seg -> seg.overlaps(queryStart, queryEnd)).collect(Collectors.toList());
			List<TestSegment> treeResult = new ArrayList<TestSegment>();
			tree.findOverlapping(queryStart, queryEnd, treeResult);

			listResult.sort(comparator);
			treeResult.sort(comparator);
			if(!listResult.equals(treeResult)) {
				System.out.println(listResult);
				System.out.println(treeResult);
				System.out.println("Mismatch found");
				break;
			} 

			if(i % 1000 == 0) {
				System.out.println(i+" of "+tests+" tests complete");
			}
		}
		
	}
	
	
	private static TestSegment testSeg(int refStart, int refEnd) {
		return new TestSegment(refStart, refEnd);
	}
	
	private static class TestSegment extends ReferenceSegment {
		public TestSegment(int refStart, int refEnd) {
			super(refStart, refEnd);
		}
		public String toString() {
			return "["+getRefStart()+", "+getRefEnd()+"]";
		}
	}
	
}
