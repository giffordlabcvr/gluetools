package uk.ac.gla.cvr.gluetools.core.segments;

import java.util.List;

public interface IQueryAlignedSegment extends IReferenceSegment {

	public Integer getQueryStart();

	public void setQueryStart(Integer queryStart);

	public Integer getQueryEnd();

	public void setQueryEnd(Integer queryEnd);

	public static double getQueryNtCoveragePercent(List<? extends IQueryAlignedSegment> alignedSegments, int queryLength) {
		int queryNTs = 0;
		for(IQueryAlignedSegment segment: alignedSegments) {
			queryNTs += 1 + Math.abs(segment.getQueryStart() - segment.getQueryEnd());
		}
		return 100.0 * queryNTs / queryLength;
	
	}

	public static double getReferenceNtCoveragePercent(List<? extends IQueryAlignedSegment> alignedSegments, int referenceLength) {
		int referenceNTs = 0;
		for(IQueryAlignedSegment segment: alignedSegments) {
			referenceNTs += 1 + Math.abs(segment.getRefStart() - segment.getRefEnd());
		}
		return 100.0 * referenceNTs / referenceLength;
	}

}