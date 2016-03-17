package uk.ac.gla.cvr.gluetools.core.segments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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

	public static <S extends IQueryAlignedSegment> List<S> sortByQueryStart(List<S> segments) {
		ArrayList<S> sorted = new ArrayList<S>(segments);
		Collections.sort(sorted, new QueryStartComparator());
		return sorted;
	}
	
	public static class QueryStartComparator implements Comparator<IQueryAlignedSegment> {
		@Override
		public int compare(IQueryAlignedSegment o1, IQueryAlignedSegment o2) {
			return Integer.compare(o1.getQueryStart(), o2.getQueryStart());
		}
	}

	public default int getReferenceToQueryOffset() {
		return getQueryStart() - getRefStart();
	}

	public default int getQueryToReferenceOffset() {
		return getRefStart() - getQueryStart();
	}

	
	public default boolean abutsRight(IQueryAlignedSegment other) {
		return IReferenceSegment.super.abutsRight(other) && other.getQueryStart() == this.getQueryEnd()+1;
	}

	public default boolean abutsLeft(IQueryAlignedSegment other) {
		return IReferenceSegment.super.abutsRight(other) && other.getQueryStart() == this.getQueryEnd()+1;
	}

	
	public default void translate(int offset) {
		setRefStart(getRefStart()+offset);
		setRefEnd(getRefEnd()+offset);
		setQueryStart(getQueryStart()+offset);
		setQueryEnd(getQueryEnd()+offset);
	}

	public default void translateRef(int offset) {
		setRefStart(getRefStart()+offset);
		setRefEnd(getRefEnd()+offset);
	}

	
}