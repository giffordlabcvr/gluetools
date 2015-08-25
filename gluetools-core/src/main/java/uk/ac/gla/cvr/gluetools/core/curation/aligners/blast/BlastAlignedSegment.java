package uk.ac.gla.cvr.gluetools.core.curation.aligners.blast;

import java.util.function.Function;

import uk.ac.gla.cvr.gluetools.core.curation.aligners.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastHsp;

public class BlastAlignedSegment extends QueryAlignedSegment {

	private BlastHsp hsp;
	public BlastAlignedSegment(int refStart, int refEnd, int queryStart, int queryEnd,
			BlastHsp hsp) {
		super(refStart, refEnd, queryStart, queryEnd);
		this.hsp = hsp;
	}
	public BlastHsp getHsp() {
		return hsp;
	}
	/**
	 * Split the segment into two parts, a new left part of length <length>
	 * which is returned
	 * This segment is then modified to be the remaining part.
	 */
	public BlastAlignedSegment truncateLeftBlast(int length) {
		checkTruncateLength(length);
		BlastAlignedSegment leftSegment = new BlastAlignedSegment(
				getRefStart(), 
				getRefStart()+length-1, 
				getQueryStart(), 
				getQueryStart()+length-1, 
				getHsp());
		setRefStart(getRefStart()+length); 
		setQueryStart(getQueryStart()+length);
		return leftSegment;
	}
	
	
	/**
	 * Split the segment into two parts, a new right part of length <length>
	 * which is returned.
	 * This segment is then modified to be the remaining part.
	 */
	public BlastAlignedSegment truncateRightBlast(int length) {
		checkTruncateLength(length);
		BlastAlignedSegment rightSegment = new BlastAlignedSegment(
				getRefEnd()-length+1, 
				getRefEnd(), 
				getQueryEnd()-length+1, 
				getQueryEnd(), 
				getHsp());
		setRefEnd(getRefEnd()-length); 
		setQueryEnd(getQueryEnd()-length);
		return rightSegment;
	}
	
	/*
	 * remove parts of new segments which overlap existing segments.
	 */
	public static void removeNewOverlaps(
			// input lists
			BlastSegmentList existingSegments,
			BlastSegmentList newSegments,
			// output lists
			BlastSegmentList existingSegmentsCopy,
			BlastSegmentList truncatedNewSegments,
			// functions controlling whether reference or query sequence overlaps are considered.
			Function<BlastAlignedSegment, Integer> getStart, 
			Function<BlastAlignedSegment, Integer> getEnd) {

		int nextNewStart = updateNextStart(newSegments, getStart);
		int nextExistingStart = updateNextStart(existingSegments, getStart);

		// single pass through both lists, the aim is to remove those new segments which overlap
		// existing segments.
		while(!(existingSegments.isEmpty() && newSegments.isEmpty())) {
			boolean anyChange;
			// pass over segments in either list while they are not overlapping
			do {
				anyChange = false;
				// while new segments are strictly to the left of remaining existing segments, 
				// add them to the truncated new segments list unchanged.
				while(!newSegments.isEmpty() &&
						getEnd.apply(newSegments.getFirst()) < nextExistingStart) {
					truncatedNewSegments.add(newSegments.removeFirst());
					nextNewStart = updateNextStart(newSegments, getStart);
					anyChange = true;
				}
				// while existing segments are strictly to the left of remaining new segments, 
				// skip them.
				while(!existingSegments.isEmpty() &&
						getEnd.apply(existingSegments.getFirst()) < nextNewStart) {
					existingSegmentsCopy.add(existingSegments.removeFirst());
					nextExistingStart = updateNextStart(existingSegments, getStart);
					anyChange = true;
				}
			} while(anyChange);
			if(!newSegments.isEmpty() && !existingSegments.isEmpty()) {
				// next new overlaps next existing.
				int existingStart = getStart.apply(existingSegments.getFirst());
				int existingEnd = getEnd.apply(existingSegments.getFirst());
				int newStart = getStart.apply(newSegments.getFirst());
				int newEnd = getEnd.apply(newSegments.getFirst());
				
				/* [ existing ---
				 *    [   new   ---
				 */
				if(existingStart <= newStart) {
					if(existingEnd < newEnd) {
						/* [1 existing 7]
						 *    [2  new     9]
						 */
						newSegments.getFirst().truncateLeft(1 + (existingEnd - newStart));
					} else {
						/* [1 existing    9]
						 *    [2  new  7]
						 */
						newSegments.removeFirst();
					}
				} else {
					/*    [ existing ---
					 * [   new   ---
					 */
					truncatedNewSegments.add(newSegments.getFirst().truncateLeftBlast(existingStart - newStart));
					if(existingEnd < newEnd) {
						/*    [3 existing 6]
						 * [1   new          9]
						 */
						newSegments.getFirst().truncateLeft(1 + (existingEnd - existingStart));
					} else {
						/*    [3 existing   9]
						 * [1   new       6]
						 */
						// nothing to do.
					}
				}
				nextNewStart = updateNextStart(newSegments, getStart);
			}
		}
	}

	
	static int updateNextStart(BlastSegmentList existingSegments, 
			Function<BlastAlignedSegment, Integer> getStart) {
		if(existingSegments.isEmpty()) {
			return Integer.MAX_VALUE;
		} else {
			return getStart.apply(existingSegments.getFirst());
		}
	}

	
	
	
	
}