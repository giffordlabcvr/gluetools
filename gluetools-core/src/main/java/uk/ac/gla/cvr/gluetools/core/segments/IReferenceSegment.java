package uk.ac.gla.cvr.gluetools.core.segments;

import java.util.List;

public interface IReferenceSegment {

	public Integer getRefStart();

	public Integer getRefEnd();

	// Alters the segments in segments1 (by truncation, or removing segments) so that only those intersecting the segments in
	// segments2 remain.
	public static void intersect(List<? extends IReferenceSegment> segments1, List<? extends IReferenceSegment> segments2) {
		
	}
	
}
