package uk.ac.gla.cvr.gluetools.core.curation.aligners.blast;

import java.util.function.Function;

import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.programs.blast.BlastHsp;

public class BlastAlignedSegment extends QueryAlignedSegment implements Cloneable {

	private BlastHsp hsp;
	public BlastAlignedSegment(int refStart, int refEnd, int queryStart, int queryEnd,
			BlastHsp hsp) {
		super(refStart, refEnd, queryStart, queryEnd);
		this.hsp = hsp;
	}
	public BlastHsp getHsp() {
		return hsp;
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
					BlastAlignedSegment newSegment = (BlastAlignedSegment) ReferenceSegment.truncateLeftSplit(newSegments.getFirst(), existingStart - newStart);
					truncatedNewSegments.add(newSegment);
					if(existingEnd < newEnd) {
						/*    [3 existing 6]
						 * [1   new          9]
						 */
						newSegments.getFirst().truncateLeft(1 + (existingEnd - existingStart));
					} else {
						/*    [3 existing   9]
						 * [1   new       6]
						 */
						newSegments.removeFirst();
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

	
	public BlastAlignedSegment clone() {
		return new BlastAlignedSegment(getRefStart(), getRefEnd(), getQueryStart(), getQueryEnd(), getHsp());
	}
	
	
}