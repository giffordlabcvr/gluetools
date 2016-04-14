package uk.ac.gla.cvr.gluetools.core.segments;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectReader;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;


public class QueryAlignedSegment extends ReferenceSegment implements Plugin, IQueryAlignedSegment, Cloneable {
	
	public static final String QUERY_START = "queryStart";
	public static final String QUERY_END = "queryEnd";

	private int queryStart, queryEnd;

	public QueryAlignedSegment(int refStart, int refEnd, int queryStart, int queryEnd) {
		super(refStart, refEnd);
		this.queryStart = queryStart;
		this.queryEnd = queryEnd;
	}
	public QueryAlignedSegment(ObjectReader objectReader) {
		super(objectReader);
		this.queryStart = objectReader.intValue(QUERY_START);
		this.queryEnd = objectReader.intValue(QUERY_END);
	}
	
	public QueryAlignedSegment(PluginConfigContext pluginConfigContext, Element configElem) {
		super();
		configure(pluginConfigContext, configElem);
	}
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext, Element configElem) {
		super.configure(pluginConfigContext, configElem);
		setQueryStart(PluginUtils.configureIntProperty(configElem, QUERY_START, true));
		setQueryEnd(PluginUtils.configureIntProperty(configElem, QUERY_END, true));
	}
	
	
	@Override
	public Integer getQueryStart() {
		return queryStart;
	}

	@Override
	public void setQueryStart(Integer queryStart) {
		this.queryStart = queryStart;
	}
	@Override
	public Integer getQueryEnd() {
		return queryEnd;
	}

	@Override
	public void setQueryEnd(Integer queryEnd) {
		this.queryEnd = queryEnd;
	}
	
	public void truncateLeft(int length) {
		super.truncateLeft(length);
		queryStart += length;
	}

	public void truncateRight(int length) {
		super.truncateRight(length);
		queryEnd -= length;
	}
	
	public QueryAlignedSegment invert() {
		return new QueryAlignedSegment(getQueryStart(), getQueryEnd(), getRefStart(), getRefEnd());
	}

	public String toString() { return
		super.toString() +
				" <-> Query: ["+getQueryStart()+", "+getQueryEnd()+"]";
	}
	
	/**
	 * returns true if the two segments propose the same offset between query and reference,
	 * 
	 * This is useful to know in the case where the reference ranges overlap: 
	 * in this case the segments can easily be merged.
	 */
	public boolean isAlignedTo(QueryAlignedSegment other) {
		return queryStart - getRefStart() == other.queryStart - other.getRefStart();
	}
	
	public void toDocument(ObjectBuilder builder) {
		super.toDocument(builder);
		builder
			.set(QUERY_START, getQueryStart())
			.set(QUERY_END, getQueryEnd());
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
	
	public static List<QueryAlignedSegment> translateSegments(
			List<QueryAlignedSegment> queryToRef1Segments0,
			List<QueryAlignedSegment> ref1ToRef2Segments0) {
		Function<QueryAlignedSegment, Integer> getRefStart = QueryAlignedSegment::getRefStart;
		Function<QueryAlignedSegment, Integer> getQueryStart = QueryAlignedSegment::getQueryStart;
		Function<QueryAlignedSegment, Integer> getRefEnd = QueryAlignedSegment::getRefEnd;
		Function<QueryAlignedSegment, Integer> getQueryEnd = QueryAlignedSegment::getQueryEnd;

		LinkedList<QueryAlignedSegment> queryToRef1Segments = 
				new LinkedList<QueryAlignedSegment>(queryToRef1Segments0.stream()
						.map(QueryAlignedSegment::clone)
						.collect(Collectors.toList()));
		LinkedList<QueryAlignedSegment> ref1ToRef2Segments = 
				new LinkedList<QueryAlignedSegment>(ref1ToRef2Segments0.stream()
						.map(QueryAlignedSegment::clone)
						.collect(Collectors.toList()));
		
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
						IQueryAlignedSegment removed = queryToRef1Segments.removeFirst();
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
						IQueryAlignedSegment removed = ref1ToRef2Segments.removeFirst();
						// System.out.println("DELETE Ref1-Ref2");
						int ref2Start = removed.getRefStart();
						QueryAlignedSegment newSeg = new QueryAlignedSegment(
								ref2Start, (ref2Start+newSegLength)-1, 
								queryStart, (queryStart+newSegLength)-1);
						// System.out.println("ADD "+newSeg);
						queryToRef2Segments.add(newSeg);
					} else {
						// both start and end line up.
						IQueryAlignedSegment removed1 = queryToRef1Segments.removeFirst();
						IQueryAlignedSegment removed2 = ref1ToRef2Segments.removeFirst();
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


	
	public QueryAlignedSegment clone() {
		return new QueryAlignedSegment(getRefStart(), getRefEnd(), queryStart, queryEnd);
	}

	public static BiFunction<QueryAlignedSegment, QueryAlignedSegment, QueryAlignedSegment> mergeAbuttingFunction() {
		return (seg1, seg2) -> {
			return new QueryAlignedSegment(seg1.getRefStart(), seg2.getRefEnd(), seg1.getQueryStart(), seg2.getQueryEnd());
		};

	}

	public static void initAllColumnsAlmt(Map<String, List<QueryAlignedSegment>> allColumnsAlmt, 
			String initialRefID, int length) {
		QueryAlignedSegment qaSeg = new QueryAlignedSegment(1, length, 1, length);
		List<QueryAlignedSegment> qaSegs = new ArrayList<QueryAlignedSegment>();
		qaSegs.add(qaSeg);
		allColumnsAlmt.put(initialRefID, qaSegs);
	}
	
	public static void updateAllColumnsAlmt(Map<String, List<QueryAlignedSegment>> allColumnsAlmt, 
			String refID, String newID, int newLen, List<QueryAlignedSegment> newToRefSegs) {
		
		List<QueryAlignedSegment> refToUSegs = allColumnsAlmt.get(refID);
		List<QueryAlignedSegment> newToUSegs = translateSegments(newToRefSegs, refToUSegs);
		
		List<QueryAlignedSegment> uToNewSegs = newToUSegs.stream()
				.map(seg -> seg.invert())
				.collect(Collectors.toList());
		uToNewSegs.sort(new Comparator<QueryAlignedSegment>() {
			@Override
			public int compare(QueryAlignedSegment o1, QueryAlignedSegment o2) {
				return Integer.compare(o1.getRefStart(), o2.getRefStart());
			}
		});
		// find those areas of the new sequence which do not have a homology with the current coordinates
		// create "unconstrained insertions" for these.
		int lastCoveredNewLoc = 0;
		int lastULoc = 0;
		List<ColumnInsertion> colInsertions = new ArrayList<ColumnInsertion>();
		for(QueryAlignedSegment uToNewSeg: uToNewSegs) {
			int newStart = lastCoveredNewLoc+1;
			if(uToNewSeg.getRefStart() > newStart) {
				ColumnInsertion colInsertion = 
						new ColumnInsertionBefore(newStart, uToNewSeg.getRefStart()-newStart, uToNewSeg.getQueryStart());
					colInsertions.add(colInsertion);
			}
			lastCoveredNewLoc = uToNewSeg.getRefEnd();
			lastULoc = uToNewSeg.getQueryEnd();
		}
		if(lastCoveredNewLoc < newLen) {
			int newStart = lastCoveredNewLoc+1;
			ColumnInsertion colInsertion = 
					new ColumnInsertionAfter(newStart, newLen-newStart, lastULoc);
				colInsertions.add(colInsertion);
		}
		// introduce the new columns into the existing unconstrained alignment.
		allColumnsAlmt.put(newID, newToUSegs);
		allColumnsAlmt.forEach( (seqId, seqToUSegs) -> {
			List<QueryAlignedSegment> newSeqToUSegs = new ArrayList<QueryAlignedSegment>(seqToUSegs);
			for(int i = 0; i < colInsertions.size(); i++) {
				ColumnInsertion colInsertion = colInsertions.get(i);
				int numCols = colInsertion.length;
				if(colInsertion instanceof ColumnInsertionBefore) {
					ColumnInsertionBefore colInsertionBefore = (ColumnInsertionBefore) colInsertion;
					newSeqToUSegs = insertRefColumnsBefore(colInsertionBefore.rightNT, numCols, newSeqToUSegs);
					colInsertion.finalRefStart = colInsertionBefore.rightNT;
					colInsertion.finalRefEnd = (colInsertionBefore.rightNT + numCols)-1;
				} else if(colInsertion instanceof ColumnInsertionAfter) {
					ColumnInsertionAfter colInsertionAfter = (ColumnInsertionAfter) colInsertion;
					newSeqToUSegs = insertRefColumnsAfter(colInsertionAfter.leftNT, numCols, newSeqToUSegs);
					colInsertion.finalRefStart = colInsertionAfter.leftNT + 1;
					colInsertion.finalRefEnd = colInsertionAfter.leftNT + numCols;
				}
				for(int j = i+1; j < colInsertions.size(); j++) {
					colInsertions.get(j).translate(numCols);
				}
			}
			seqToUSegs.clear();
			seqToUSegs.addAll(newSeqToUSegs);
		} );
		
		// add new segments for these regions for the new sequence.
		for(ColumnInsertion colInsertion: colInsertions) {
			newToUSegs.add(new QueryAlignedSegment(colInsertion.finalRefStart, colInsertion.finalRefEnd, 
					colInsertion.newStart, (colInsertion.newStart+colInsertion.length)-1));
		}
		newToUSegs.sort(new Comparator<QueryAlignedSegment>() {
			@Override
			public int compare(QueryAlignedSegment o1, QueryAlignedSegment o2) {
				return Integer.compare(o1.getRefStart(), o2.getRefStart());
			}
		});
	}
		
	
	private static abstract class ColumnInsertion {
		int newStart;
		int length;
		int finalRefStart;
		int finalRefEnd;
		public ColumnInsertion(int newStart, int length) {
			super();
			this.newStart = newStart;
			this.length = length;
		}
		public abstract void translate(int amount);
	}
	
	private static class ColumnInsertionBefore extends ColumnInsertion {
		int rightNT;
		public ColumnInsertionBefore(int newStart, int length, int rightNT) {
			super(newStart, length);
			this.rightNT = rightNT;
		}
		@Override
		public void translate(int amount) {
			rightNT += amount;
		}
	}

	private static class ColumnInsertionAfter extends ColumnInsertion {
		int leftNT;
		public ColumnInsertionAfter(int newStart, int length, int leftNT) {
			super(newStart, length);
			this.leftNT = leftNT;
		}
		@Override
		public void translate(int amount) {
			leftNT += amount;
		}
	}

	public static List<QueryAlignedSegment> insertRefColumnsBefore(int beforeCoord, int numCols, List<QueryAlignedSegment> qaSegs) {
		List<QueryAlignedSegment> resultQaSegs = new ArrayList<QueryAlignedSegment>();
		for(QueryAlignedSegment qaSeg: qaSegs) {
			if(qaSeg.getRefEnd() < beforeCoord) {
				resultQaSegs.add(qaSeg.clone());
			} else if(qaSeg.getRefStart() >= beforeCoord) {
				QueryAlignedSegment resultQaSeg = qaSeg.clone();
				resultQaSeg.translateRef(numCols);
				resultQaSegs.add(resultQaSeg);
			} else {
				QueryAlignedSegment resultQaLeftSeg = qaSeg.clone();
				resultQaLeftSeg.truncateRight((qaSeg.getRefEnd() - beforeCoord)+1);
				resultQaSegs.add(resultQaLeftSeg);

				QueryAlignedSegment resultQaRightSeg = qaSeg.clone();
				resultQaRightSeg.truncateLeft(beforeCoord - qaSeg.getRefStart()); 
				resultQaRightSeg.translateRef(numCols);
				resultQaSegs.add(resultQaRightSeg);
			}
		}
		return resultQaSegs;
	}

	
	public static List<QueryAlignedSegment> insertRefColumnsAfter(int afterCoord, int numCols, List<QueryAlignedSegment> qaSegs) {
		List<QueryAlignedSegment> resultQaSegs = new ArrayList<QueryAlignedSegment>();
		for(QueryAlignedSegment qaSeg: qaSegs) {
			if(qaSeg.getRefEnd() <= afterCoord) {
				resultQaSegs.add(qaSeg.clone());
			} else if(qaSeg.getRefStart() > afterCoord) {
				QueryAlignedSegment resultQaSeg = qaSeg.clone();
				resultQaSeg.translateRef(numCols);
				resultQaSegs.add(resultQaSeg);
			} else {
				QueryAlignedSegment resultQaLeftSeg = qaSeg.clone();
				resultQaLeftSeg.truncateRight(qaSeg.getRefEnd() - afterCoord);
				resultQaSegs.add(resultQaLeftSeg);

				QueryAlignedSegment resultQaRightSeg = qaSeg.clone();
				resultQaRightSeg.truncateLeft((afterCoord - qaSeg.getRefStart())+1); 
				resultQaRightSeg.translateRef(numCols);
				resultQaSegs.add(resultQaRightSeg);
			}
		}
		return resultQaSegs;
	}

	
}