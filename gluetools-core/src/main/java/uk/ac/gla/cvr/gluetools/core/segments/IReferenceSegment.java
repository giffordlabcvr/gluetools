package uk.ac.gla.cvr.gluetools.core.segments;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public interface IReferenceSegment {

	public Integer getRefStart();

	public Integer getRefEnd();

	/** 
	 * Alter the segments in segments1 (by truncation, or removing segments) so that only those intersecting the segments in
	 * segments2 remain.
	 * 
	 * @param segments1
	 * @param segments2
	 */
	public static void intersect(List<? extends IReferenceSegment> segments1, List<? extends IReferenceSegment> segments2) {
		
	}

	/** 
	 * given two lists of segments, and a function which merges segments, generate new segments which cover 
	 * those locations in both lists.
	 * A segment in the returned list will have been transformed by the segMerger function.
	 * in both lists.
	 */
	public static <N extends IReferenceSegment,
				   SA extends IReferenceSegment,
				   SB extends IReferenceSegment> List<N> 
		intersection(List<SA> segments1, List<SB> segments2, BiFunction<SA, SB, N> segMerger) {
		
		List<N> newSegments = new ArrayList<N>();
		
		return newSegments;
	}

	/** 
	 * Given two lists of segments and functions which merge and transform segments, 
	 * generate new segments which cover those locations in either list.
	 * A segment in the returned list will have been transformed by segTransformerA if it covers
	 * only locations in segments1. It will have been transformed by segTransformerB if it covers
	 * only locations in segments2. It will have been transformed by segMerger if it covers
	 * locations in both segments1 and segments2.
	 * @return
	 */
	public static <N extends IReferenceSegment,
	   SA extends IReferenceSegment,
	   SB extends IReferenceSegment> List<N> 
		union(List<SA> segments1, List<SB> segments2, 
				BiFunction<SA, SB, N> segMerger, 
				Function<SA, N> segTransformerA,
				Function<SB, N> segTransformerB) {

		List<N> newSegments = new ArrayList<N>();
		return newSegments;
	}
	
}
