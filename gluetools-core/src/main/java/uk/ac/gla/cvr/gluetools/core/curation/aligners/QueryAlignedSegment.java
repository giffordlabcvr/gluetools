package uk.ac.gla.cvr.gluetools.core.curation.aligners;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.function.Function;

import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectReader;


public class QueryAlignedSegment {
	private int refStart, refEnd, queryStart, queryEnd;

	public QueryAlignedSegment(int refStart, int refEnd, int queryStart, int queryEnd) {
		super();
		this.refStart = refStart;
		this.refEnd = refEnd;
		this.queryStart = queryStart;
		this.queryEnd = queryEnd;
	}
	public QueryAlignedSegment(ObjectReader objectReader) {
		this(objectReader.intValue("refStart"),
				objectReader.intValue("refEnd"),
				objectReader.intValue("queryStart"),
				objectReader.intValue("queryEnd"));
	}
	
	public int getRefStart() {
		return refStart;
	}
	public void setRefStart(int refStart) {
		this.refStart = refStart;
	}
	public int getRefEnd() {
		return refEnd;
	}
	public void setRefEnd(int refEnd) {
		this.refEnd = refEnd;
	}
	public int getQueryStart() {
		return queryStart;
	}
	public void setQueryStart(int queryStart) {
		this.queryStart = queryStart;
	}
	public int getQueryEnd() {
		return queryEnd;
	}
	public void setQueryEnd(int queryEnd) {
		this.queryEnd = queryEnd;
	}
	
	public void truncateLeft(int length) {
		checkTruncateLength(length);
		queryStart+=length;
		refStart+=length;
	}
	
	protected void checkTruncateLength(int length) {
		if(length <= 0 || length > getRefEnd() - getRefStart()) {
			throw new IllegalArgumentException("Illegal length argument: "+
		length+": should be between "+1+" and "+(getRefEnd() - getRefStart())+" inclusive" );
		}
	}

	public String toString() { return
		"Ref: ["+getRefStart()+", "+getRefEnd()+"] "+
				"<-> Query: ["+getQueryStart()+", "+getQueryEnd()+"]";
	}
	
	/**
	 * returns true if the two segments propose the same offset between query and reference,
	 * 
	 * This is useful to know in the case where the reference ranges overlap: 
	 * in this case the segments can easily be merged.
	 */
	public boolean isAlignedTo(QueryAlignedSegment other) {
		return queryStart - refStart == other.queryStart - other.refStart;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + queryEnd;
		result = prime * result + queryStart;
		result = prime * result + refEnd;
		result = prime * result + refStart;
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		QueryAlignedSegment other = (QueryAlignedSegment) obj;
		if (queryEnd != other.queryEnd)
			return false;
		if (queryStart != other.queryStart)
			return false;
		if (refEnd != other.refEnd)
			return false;
		if (refStart != other.refStart)
			return false;
		return true;
	}

	public void toDocument(ObjectBuilder builder) {
		builder
			.set("refStart", getRefStart())
			.set("refEnd", getRefEnd())
			.set("queryStart", getQueryStart())
			.set("queryEnd", getQueryEnd());
	}
	
	private static class SegmentStartComparator implements Comparator<QueryAlignedSegment> {
		private Function<QueryAlignedSegment, Integer> getStart;
		public SegmentStartComparator(
				Function<QueryAlignedSegment, Integer> getStart) {
			super();
			this.getStart = getStart;
		}
		@Override
		public int compare(QueryAlignedSegment o1, QueryAlignedSegment o2) {
			return Integer.compare(getStart.apply(o1), getStart.apply(o2));
		}
		
	}
	
	public static LinkedList<QueryAlignedSegment> translateSegments(
			LinkedList<QueryAlignedSegment> queryToRef1Segments,
			LinkedList<QueryAlignedSegment> ref1ToRef2Segments) {
		Function<QueryAlignedSegment, Integer> getRefStart = QueryAlignedSegment::getRefStart;
		Function<QueryAlignedSegment, Integer> getQueryStart = QueryAlignedSegment::getQueryStart;
		Function<QueryAlignedSegment, Integer> getRefEnd = QueryAlignedSegment::getRefEnd;
		Function<QueryAlignedSegment, Integer> getQueryEnd = QueryAlignedSegment::getQueryEnd;

		Collections.sort(queryToRef1Segments, new SegmentStartComparator(getRefStart));
		Collections.sort(ref1ToRef2Segments, new SegmentStartComparator(getQueryStart));
		
		int queryToRef1NextStart, ref1ToRef2NextStart, 
			queryToRef1NextEnd, ref1ToRef2NextEnd;	
		LinkedList<QueryAlignedSegment> queryToRef2Segments = new LinkedList<QueryAlignedSegment>();
		while(!queryToRef1Segments.isEmpty() && !ref1ToRef2Segments.isEmpty()) {
			queryToRef1NextStart = updateNext(queryToRef1Segments, getRefStart);
			ref1ToRef2NextStart = updateNext(ref1ToRef2Segments, getQueryStart);
			queryToRef1NextEnd = updateNext(queryToRef1Segments, getRefEnd);
			ref1ToRef2NextEnd = updateNext(ref1ToRef2Segments, getQueryEnd);
			
			// System.out.println("--------");
			// System.out.println("Ref1-Ref2:::"+ref1ToRef2Segments.getFirst());
			// System.out.println("Query-Ref1:::"+queryToRef1Segments.getFirst());
			
			if(ref1ToRef2NextEnd < queryToRef1NextStart) {
				ref1ToRef2Segments.removeFirst();
				// System.out.println("DELETE Ref1-Ref2");
			} else if(queryToRef1NextEnd < ref1ToRef2NextStart) {
				queryToRef1Segments.removeFirst();
				// System.out.println("DELETE Query-Ref1");
			} else {
				// some kind of overlap.
				int startDiff = ref1ToRef2NextStart - queryToRef1NextStart ;
				if(startDiff > 0) {
					queryToRef1Segments.getFirst().truncateLeft(startDiff);
					// System.out.println("TRUNCATE Query-Ref1");
				} else if(startDiff < 0) {
					ref1ToRef2Segments.getFirst().truncateLeft(-startDiff);
					// System.out.println("TRUNCATE Ref1-Ref2");
				} else {
					// starts line up
					if(queryToRef1NextEnd < ref1ToRef2NextEnd) {
						int ref2Start = ref1ToRef2Segments.getFirst().getRefStart();
						int newSegLength = ( queryToRef1NextEnd - queryToRef1NextStart ) + 1;
						ref1ToRef2Segments.getFirst().truncateLeft(newSegLength);
						// System.out.println("TRUNCATE Ref1-Ref2");
						QueryAlignedSegment removed = queryToRef1Segments.removeFirst();
						// System.out.println("DELETE Query-Ref1");
						int queryStart = removed.getQueryStart();
						QueryAlignedSegment newSeg = new QueryAlignedSegment(
								ref2Start, (ref2Start+newSegLength)-1, 
								queryStart, (queryStart+newSegLength)-1);
						// System.out.println("ADD "+newSeg);
						queryToRef2Segments.add(newSeg);
					} else if(ref1ToRef2NextEnd < queryToRef1NextEnd) {
						int newSegLength = ( ref1ToRef2NextEnd - ref1ToRef2NextStart ) + 1;
						int queryStart = queryToRef1Segments.getFirst().getQueryStart();
						queryToRef1Segments.getFirst().truncateLeft(newSegLength);
						// System.out.println("TRUNCATE Query-Ref1");
						QueryAlignedSegment removed = ref1ToRef2Segments.removeFirst();
						// System.out.println("DELETE Ref1-Ref2");
						int ref2Start = removed.getRefStart();
						QueryAlignedSegment newSeg = new QueryAlignedSegment(
								ref2Start, (ref2Start+newSegLength)-1, 
								queryStart, (queryStart+newSegLength)-1);
						// System.out.println("ADD "+newSeg);
						queryToRef2Segments.add(newSeg);
					} else {
						// both start and end line up.
						QueryAlignedSegment removed1 = queryToRef1Segments.removeFirst();
						QueryAlignedSegment removed2 = ref1ToRef2Segments.removeFirst();
						// System.out.println("DELETE Query-Ref1");
						// System.out.println("DELETE Ref1-Ref2");
						QueryAlignedSegment newSeg = new QueryAlignedSegment(
								removed2.getRefStart(), removed2.getRefEnd(), 
								removed1.getQueryStart(), removed1.getQueryEnd());
						// System.out.println("ADD "+newSeg);
						queryToRef2Segments.add(newSeg);
					}
				}
			}
		}
		
		return queryToRef2Segments;
	}
	
	private static int updateNext(
			LinkedList<QueryAlignedSegment> alignedSegments,
			Function<QueryAlignedSegment, Integer> getStart) {
		if(alignedSegments.isEmpty()) {
			return Integer.MAX_VALUE;
		}
		return getStart.apply(alignedSegments.getFirst());
	}
	
	
}