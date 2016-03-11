package uk.ac.gla.cvr.gluetools.core.segments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.document.ObjectReader;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public class ReferenceSegment implements Plugin, IReferenceSegment, Cloneable {

	
	private int refStart, refEnd;

	public ReferenceSegment(int refStart, int refEnd) {
		super();
		this.refStart = refStart;
		this.refEnd = refEnd;
	}
	public ReferenceSegment(ObjectReader objectReader) {
		this(objectReader.intValue(REF_START),
				objectReader.intValue(REF_END));
	}
	
	public ReferenceSegment(PluginConfigContext pluginConfigContext,
			Element configElem) {
		configure(pluginConfigContext, configElem);
	}
	
	protected ReferenceSegment() {
	}
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		setRefStart(PluginUtils.configureIntProperty(configElem, REF_START, true));
		setRefEnd(PluginUtils.configureIntProperty(configElem, REF_END, true));
	}
	
	@Override
	public Integer getRefStart() {
		return refStart;
	}
	public void setRefStart(Integer refStart) {
		this.refStart = refStart;
	}
	@Override
	public Integer getRefEnd() {
		return refEnd;
	}
	
	public void setRefEnd(Integer refEnd) {
		this.refEnd = refEnd;
	}
	

	public String toString() { return
		"Ref: ["+getRefStart()+", "+getRefEnd()+"]";
	}

	
	public ReferenceSegment clone() {
		return new ReferenceSegment(refStart, refEnd);
	}
	
	/**
	 * Split a segment into two parts, a new left part of length <length>
	 * which is returned.
	 * The supplied segment is then modified to be the remaining part.
	 */
	public static <A extends IReferenceSegment> A truncateLeftSplit(A segment, int length) {
		@SuppressWarnings("unchecked")
		A newSegment = (A) segment.clone();
		int currentLength = segment.getCurrentLength();
		segment.truncateLeft(length);
		newSegment.truncateRight(currentLength-length);
		return newSegment;
	}
	
	/**
	 * Split a segment into two parts, a new right part of length <length>
	 * which is returned.
	 * The supplied segment is modified to be the remaining part.
	 */
	public static <A extends IReferenceSegment> A truncateRightSplit(A segment, int length) {
		@SuppressWarnings("unchecked")
		A newSegment = (A) segment.clone();
		int currentLength = segment.getCurrentLength();
		segment.truncateRight(length);
		newSegment.truncateLeft(currentLength-length);
		return newSegment;
	}
	

	
	/** 
	 * given two lists of segments segments1 and segments2
	 * 
	 * return a list of new segments which cover those locations which are covered by segments
	 * in segments1 but not covered by segments in segments2. 
	 * 
	 * The returned segments will habe been cloned from segments in list segments1.
	 */
	public static <SA extends IReferenceSegment,
	   SB extends IReferenceSegment> List<SA> 
	subtract(List<SA> segments1, List<SB> segments2) {

		LinkedList<SA> segments1Linked = cloneSegmentList(segments1);
		LinkedList<SB> segments2Linked = cloneSegmentList(segments2);

		int nextSeg1Start = updateNextStart(segments1Linked);
		int nextSeg2Start = updateNextStart(segments2Linked);
		
		List<SA> resultSegments = new ArrayList<SA>();

		while(!segments1Linked.isEmpty() || !segments2Linked.isEmpty()) {

			boolean anyChange;
			// add clones from segments in either list while they are not overlapping
			do {
				anyChange = false;
				// while segment1s are strictly to the left of remaining segment2s, 
				// add to the result. 
				while(!segments1Linked.isEmpty() &&
						segments1Linked.getFirst().getRefEnd() < nextSeg2Start) {
					SA seg1 = segments1Linked.removeFirst();
					resultSegments.add(seg1);
					nextSeg1Start = updateNextStart(segments1Linked);
					anyChange = true;
				}
				// while segment2s are strictly to the left of remaining segment1s, 
				// skip them 
				while(!segments2Linked.isEmpty() &&
						segments2Linked.getFirst().getRefEnd() < nextSeg1Start) {
					segments2Linked.removeFirst();
					nextSeg2Start = updateNextStart(segments2Linked);
					anyChange = true;
				}
			} while(anyChange);

			if(!segments1Linked.isEmpty() && !segments2Linked.isEmpty()) {
				SA seg1 = segments1Linked.getFirst();
				SB seg2 = segments2Linked.getFirst();

				int seg1Start = nextSeg1Start;
				int seg1End = seg1.getRefEnd();
				int seg2Start = nextSeg2Start;
				int seg2End = seg2.getRefEnd();

				/* [   seg1   ---
				 *    [  seg2   ---
				 */
				if(seg1Start < seg2Start) {
					if(seg1End < seg2End) {
						/* [1   seg1   7]
						 *    [2 seg2     9]
						 */
						segments1Linked.removeFirst();
						seg2.truncateLeft(1 + (seg1End - seg2Start));
						seg1.truncateRight(1 + (seg1End - seg2Start));
						resultSegments.add(seg1);
					} else if(seg1End == seg2End) {
						/* [1   seg1   7]
						 *    [2 seg2  7]
						 */
						segments1Linked.removeFirst();
						segments2Linked.removeFirst();
						seg1.truncateRight(1 + (seg1End - seg2Start));
						resultSegments.add(seg1);
					} else {
						/* [1   seg1      9]
						 *    [2 seg2  7]
						 */
						resultSegments.add(truncateLeftSplit(seg1, seg2Start - seg1Start));
						seg1.truncateLeft(1 + (seg2End - seg2Start));
						segments2Linked.removeFirst();
					}
				} else {
					/*    [   seg1   ---
					 * [  seg2   ---
					 */
					if(seg1End < seg2End) {
						/*    [3   seg1   6]
						 * [1  seg2          9]
						 */
						segments1Linked.removeFirst();
						seg2.truncateLeft(1 + (seg1End - seg2Start));
					} else if(seg1End == seg2End) {
						/*    [3   seg1   6]
						 * [1  seg2       6]
						 */
						segments1Linked.removeFirst();
						segments2Linked.removeFirst();
					} else {
						/*    [3   seg1     9]
						 * [1  seg2       6]
						 */
						seg1.truncateLeft(1 + (seg2End - seg1Start));
						segments2Linked.removeFirst();
					}
				}
				nextSeg1Start = updateNextStart(segments1Linked);
				nextSeg2Start = updateNextStart(segments2Linked);
			}
		}

		return resultSegments;
	}
	
	
	
	/** 
	 * given two lists of segments, and a function which generates a new segment
	 * equal to the overlapping region from two overlapping segments, 
	 * return a list of new segments which cover those locations which are covered by both lists.
	 * A segment in the returned list will have been generated by the segMerger function.
	 * The segMerger function must operate like this:
	 * 
	 * SA seg1;
	 * SB seg2;
	 * N merged = segMerger(seg1, seg2);
	 * where:
	 *   merged.getRefStart() == Math.max(seg1.getRefStart(), seg2.getRefStart())
	 *   merged.getRefEnd() == Math.min(seg1.getRefEnd(), seg2.getRefEnd())
	 */
	public static <N extends IReferenceSegment,
				   SA extends IReferenceSegment,
				   SB extends IReferenceSegment> List<N> 
	intersection(List<SA> segments1, List<SB> segments2, BiFunction<SA, SB, N> segMerger) {

		LinkedList<SA> segments1Linked = cloneSegmentList(segments1);
		LinkedList<SB> segments2Linked = cloneSegmentList(segments2);

		List<N> intersectionSegments = new ArrayList<N>();

		int nextSeg1Start = updateNextStart(segments1Linked);
		int nextSeg2Start = updateNextStart(segments2Linked);

		while(!segments1Linked.isEmpty() && !segments2Linked.isEmpty()) {

			boolean anyChange;
			// pass over segments in either list while they are not overlapping
			do {
				anyChange = false;
				// while segment1s are strictly to the left of remaining segment2s, 
				// skip them 
				while(!segments1Linked.isEmpty() &&
						segments1Linked.getFirst().getRefEnd() < nextSeg2Start) {
					segments1Linked.removeFirst();
					nextSeg1Start = updateNextStart(segments1Linked);
					anyChange = true;
				}
				// while segment2s are strictly to the left of remaining segment1s, 
				// skip them 
				while(!segments2Linked.isEmpty() &&
						segments2Linked.getFirst().getRefEnd() < nextSeg1Start) {
					segments2Linked.removeFirst();
					nextSeg2Start = updateNextStart(segments2Linked);
					anyChange = true;
				}
			} while(anyChange);

			if(!segments1Linked.isEmpty() && !segments2Linked.isEmpty()) {
				SA seg1 = segments1Linked.getFirst();
				SB seg2 = segments2Linked.getFirst();

				int seg1Start = nextSeg1Start;
				int seg1End = seg1.getRefEnd();
				int seg2Start = nextSeg2Start;
				int seg2End = seg2.getRefEnd();

				N mergedSegment = segMerger.apply(seg1, seg2);
				if(mergedSegment.getRefStart() != Math.max(seg1Start, seg2Start)) {
					throw new RuntimeException("Merged segment has incorrect refStart");
				}
				if(mergedSegment.getRefEnd() != Math.min(seg1End, seg2End)) {
					throw new RuntimeException("Merged segment has incorrect refEnd");
				}

				intersectionSegments.add(mergedSegment);

				/* [   seg1   ---
				 *    [  seg2   ---
				 */
				if(seg1Start <= seg2Start) {
					if(seg1End < seg2End) {
						/* [1   seg1   7]
						 *    [2 seg2     9]
						 */
						segments1Linked.removeFirst();
						seg2.truncateLeft(1 + (seg1End - seg2Start));
					} else if(seg1End == seg2End) {
						/* [1   seg1   7]
						 *    [2 seg2  7]
						 */
						segments1Linked.removeFirst();
						segments2Linked.removeFirst();
					} else {
						/* [1   seg1      9]
						 *    [2 seg2  7]
						 */
						seg1.truncateLeft(1 + (seg2End - seg1Start));
						segments2Linked.removeFirst();
					}
				} else {
					/*    [   seg1   ---
					 * [  seg2   ---
					 */
					if(seg1End < seg2End) {
						/*    [3   seg1   6]
						 * [1  seg2          9]
						 */
						segments1Linked.removeFirst();
						seg2.truncateLeft(1 + (seg1End - seg2Start));
					} else if(seg1End == seg2End) {
						/*    [3   seg1   6]
						 * [1  seg2       6]
						 */
						segments1Linked.removeFirst();
						segments2Linked.removeFirst();
					} else {
						/*    [3   seg1     9]
						 * [1  seg2       6]
						 */
						seg1.truncateLeft(1 + (seg2End - seg1Start));
						segments2Linked.removeFirst();
					}
				}
				nextSeg1Start = updateNextStart(segments1Linked);
				nextSeg2Start = updateNextStart(segments2Linked);
			}
		}


		return intersectionSegments;
	}
	
	public static Integer minRefStart(List<? extends IReferenceSegment> segList) {
		return segList.stream().map(s -> s.getRefStart()).min(Integer::compare).orElse(null);
	}

	public static Integer maxRefEnd(List<? extends IReferenceSegment> segList) {
		return segList.stream().map(s -> s.getRefEnd()).max(Integer::compare).orElse(null);
	}

	private static <S extends IReferenceSegment> LinkedList<S> cloneSegmentList(
			List<S> segments1) {
		LinkedList<S> segments1Linked = new LinkedList<S>();
		for(S seg1: segments1) {
			@SuppressWarnings("unchecked")
			S seg1Copy = (S) seg1.clone();
			segments1Linked.add(seg1Copy);
		}
		return segments1Linked;
	}
	
	public static boolean coversLocation(List<? extends IReferenceSegment> segList, int location) {
		for(IReferenceSegment seg: segList) {
			if(location >= seg.getRefStart() && location <= seg.getRefEnd()) {
				return true;
			}
		}
		return false;
	}
	
	private static int updateNextStart(LinkedList<? extends IReferenceSegment> segList) {
		if(segList.isEmpty()) {
			return Integer.MAX_VALUE;
		}
		return segList.getFirst().getRefStart();
	}

	public static <SA extends IReferenceSegment, SB extends IReferenceSegment> BiFunction<SA, SB, SA> cloneLeftSegMerger() {
		return new BiFunction<SA, SB, SA>() {
			@Override
			public SA apply(SA leftSeg, SB rightSeg) {
				@SuppressWarnings("unchecked")
				SA overlap = (SA) leftSeg.clone();
				int leftTruncate = Math.max(leftSeg.getRefStart(), rightSeg.getRefStart()) - leftSeg.getRefStart();
				if(leftTruncate > 0) {
					overlap.truncateLeft(leftTruncate);
				}
				int rightTruncate = leftSeg.getRefEnd() - Math.min(leftSeg.getRefEnd(), rightSeg.getRefEnd());
				if(rightTruncate > 0) {
					overlap.truncateRight(rightTruncate);
				}
				return overlap;
			}
		};
	}

	
	public static <SA extends IReferenceSegment, SB extends IReferenceSegment> BiFunction<SA, SB, SB> cloneRightSegMerger() {
		return new BiFunction<SA, SB, SB>() {
			@Override
			public SB apply(SA leftSeg, SB rightSeg) {
				@SuppressWarnings("unchecked")
				SB overlap = (SB) rightSeg.clone();
				int leftTruncate = Math.max(leftSeg.getRefStart(), rightSeg.getRefStart()) - rightSeg.getRefStart();
				if(leftTruncate > 0) {
					overlap.truncateLeft(leftTruncate);
				}
				int rightTruncate = rightSeg.getRefEnd() - Math.min(leftSeg.getRefEnd(), rightSeg.getRefEnd());
				if(rightTruncate > 0) {
					overlap.truncateRight(rightTruncate);
				}
				return overlap;
			}
		};
	}
	
	public static boolean sameRegion(
			List<? extends IReferenceSegment> segments1, 
			List<? extends IReferenceSegment> segments2) {
		return covers(segments1, segments2) && covers(segments2, segments1);
	}
	
	/**
     * returns true iff every segment in segmentsToCover is covered by segments.
	 * @param segments
	 * @param segmentsToCover
	 * @return
	 */
	public static boolean covers(List<? extends IReferenceSegment> segments, 
			List<? extends IReferenceSegment> segmentsToCover) {
		LinkedList<IReferenceSegment> segmentsCopy = new LinkedList<IReferenceSegment>(segments);
		LinkedList<ReferenceSegment> segmentsToCoverCopy = new LinkedList<ReferenceSegment>();
		segmentsToCover.forEach(seg -> segmentsToCoverCopy.add(new ReferenceSegment(seg.getRefStart(), seg.getRefEnd())));
		
		while(!segmentsCopy.isEmpty() && !segmentsToCoverCopy.isEmpty()) {
			if(segmentsCopy.isEmpty() && !segmentsToCoverCopy.isEmpty()) {
				return false;
			}
			if(!segmentsCopy.isEmpty() && segmentsToCoverCopy.isEmpty()) {
				return true;
			}
			Integer segRefStart = segmentsCopy.getFirst().getRefStart();
			Integer segRefEnd = segmentsCopy.getFirst().getRefEnd();
			Integer seg2coverRefStart = segmentsToCoverCopy.getFirst().getRefStart();
			Integer seg2coverRefEnd = segmentsToCoverCopy.getFirst().getRefEnd();
			if(segRefEnd < seg2coverRefStart) {
				segmentsCopy.removeFirst(); // first in segments is irrelevant, remove it.
			} else if(seg2coverRefEnd < segRefStart) {
				return false; // first in segmentsToCover is uncovered.
			} else if(segRefStart <= seg2coverRefStart) {
				/* [1   seg  ....
				 *    [2 seg2cover ....
				 */
				if(seg2coverRefEnd <= segRefEnd) {
					/* [1   seg           9]
					 *    [2 seg2cover 7]
					 */
					segmentsToCoverCopy.removeFirst(); // seg2cover contained
				} else {
					/* [1   seg      7]
					 *    [2 seg2cover  9]
					 */
					segmentsCopy.removeFirst(); 
					segmentsToCoverCopy.getFirst().truncateLeft((segRefEnd - seg2coverRefStart) + 1 ); 
				}
			} else {
				/*    [2   seg  ....
				 * [1    seg2cover ....
				 */
				return false; // some part of seg2cover is uncovered.
			}
		}
		if(segmentsCopy.isEmpty() && !segmentsToCoverCopy.isEmpty()) {
			return false;
		}
		if(!segmentsCopy.isEmpty() && segmentsToCoverCopy.isEmpty()) {
			return true;
		}
		return true; // not sure if this is reachable!
	}


	
	public static <SA extends IReferenceSegment> List<SA> mergeAbutting(List<SA> segments, BiFunction<SA, SA, SA> segMerger) {
		if(segments.isEmpty()) {
			return Collections.emptyList();
		}
		LinkedList<SA> segmentsCopy = new LinkedList<SA>(segments);
		ArrayList<SA> result = new ArrayList<SA>();
		SA currentMerged = segmentsCopy.remove(0);
		while(!segmentsCopy.isEmpty()) {
			SA next = segmentsCopy.remove(0);
			if(currentMerged.abutsRight(next)) {
				currentMerged = segMerger.apply(currentMerged, next);
			} else {
				result.add(currentMerged);
				currentMerged = next;
			}
		}
		result.add(currentMerged);
		return result;
	}
	
}
