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
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.document.CommandObject;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;
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
	public QueryAlignedSegment(CommandObject commandObject) {
		super(commandObject);
		this.queryStart = commandObject.getInteger(QUERY_START);
		this.queryEnd = commandObject.getInteger(QUERY_END);
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
	
	public void toDocument(CommandObject builder) {
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
	
	
	public static List<QueryAlignedSegment> invertList(List<QueryAlignedSegment> queryAlignedSegs) {
		return queryAlignedSegs.stream().map(seg -> seg.invert()).collect(Collectors.toList());
	}
	
	public static <SA extends QueryAlignedSegment,
	   				SB extends QueryAlignedSegment> List<SA> 
	translateSegments(List<SA> queryToRef1Segments0, List<SB> ref1ToRef2Segments0) {
		Function<QueryAlignedSegment, Integer> getRefStart = QueryAlignedSegment::getRefStart;
		Function<QueryAlignedSegment, Integer> getQueryStart = QueryAlignedSegment::getQueryStart;
		Function<QueryAlignedSegment, Integer> getRefEnd = QueryAlignedSegment::getRefEnd;
		Function<QueryAlignedSegment, Integer> getQueryEnd = QueryAlignedSegment::getQueryEnd;

		@SuppressWarnings("unchecked")
		LinkedList<SA> queryToRef1Segments = 
				new LinkedList<SA>((List<SA>) queryToRef1Segments0.stream()
						.map(seg -> seg.clone())
						.collect(Collectors.toList()));
		
		@SuppressWarnings("unchecked")
		LinkedList<SB> ref1ToRef2Segments = 
				new LinkedList<SB>((List<SB>) ref1ToRef2Segments0.stream()
						.map(seg -> seg.clone())
						.collect(Collectors.toList()));
		
		Collections.sort(queryToRef1Segments, new SegmentStartComparator(getRefStart));
		Collections.sort(ref1ToRef2Segments, new SegmentStartComparator(getQueryStart));
		
		int queryToRef1NextStart, ref1ToRef2NextStart, 
			queryToRef1NextEnd, ref1ToRef2NextEnd;	
		LinkedList<SA> queryToRef2Segments = new LinkedList<SA>();
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
						SA removed = queryToRef1Segments.removeFirst();
						// System.out.println("DELETE Query-Ref1");
						int queryStart = removed.getQueryStart();
						@SuppressWarnings("unchecked")
						SA newSeg = (SA) removed.clone(); 
						newSeg.setRefStart(ref2Start);
						newSeg.setRefEnd((ref2Start+newSegLength)-1);
						newSeg.setQueryStart(queryStart);
						newSeg.setQueryEnd((queryStart+newSegLength)-1);
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
						@SuppressWarnings("unchecked")
						SA newSeg = (SA) queryToRef1Segments.getFirst().clone(); 
						newSeg.setRefStart(ref2Start);
						newSeg.setRefEnd((ref2Start+newSegLength)-1);
						newSeg.setQueryStart(queryStart);
						newSeg.setQueryEnd((queryStart+newSegLength)-1);
						// System.out.println("ADD "+newSeg);
						queryToRef2Segments.add(newSeg);
					} else {
						// both start and end line up.
						SA removed1 = queryToRef1Segments.removeFirst();
						IQueryAlignedSegment removed2 = ref1ToRef2Segments.removeFirst();
						// System.out.println("DELETE Query-Ref1");
						// System.out.println("DELETE Ref1-Ref2");
						@SuppressWarnings("unchecked")
						SA newSeg = (SA) removed1.clone(); 
						newSeg.setRefStart(removed2.getRefStart());
						newSeg.setRefEnd(removed2.getRefEnd());
						newSeg.setQueryStart(removed1.getQueryStart());
						newSeg.setQueryEnd(removed1.getQueryEnd());
						// System.out.println("ADD "+newSeg);
						queryToRef2Segments.add(newSeg);
					}
				}
			}
		}
		
		return queryToRef2Segments;
	}
	
	private static <S extends QueryAlignedSegment> int updateNext(
			LinkedList<S> alignedSegments,
			Function<QueryAlignedSegment, Integer> getStart) {
		if(alignedSegments.isEmpty()) {
			return Integer.MAX_VALUE;
		}
		return getStart.apply(alignedSegments.getFirst());
	}


	
	public QueryAlignedSegment clone() {
		return new QueryAlignedSegment(getRefStart(), getRefEnd(), queryStart, queryEnd);
	}

	public static BiFunction<QueryAlignedSegment, QueryAlignedSegment, QueryAlignedSegment> mergeAbuttingFunctionQueryAlignedSegment() {
		return (seg1, seg2) -> {
			return new QueryAlignedSegment(seg1.getRefStart(), seg2.getRefEnd(), seg1.getQueryStart(), seg2.getQueryEnd());
		};
	}

	public static <S extends QueryAlignedSegment> BiPredicate<S, S> abutsPredicateQueryAlignedSegment() {
		return (seg1, seg2) -> {
			return seg2.getRefStart() == seg1.getRefEnd()+1 && seg2.getQueryStart() == seg1.getQueryEnd()+1;
		};
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

	
	public static List<QueryAlignedSegment> deleteRefColumnsAfter(int afterCoord, int numCols, List<QueryAlignedSegment> qaSegs) {
		List<QueryAlignedSegment> resultQaSegs = new ArrayList<QueryAlignedSegment>();
		for(QueryAlignedSegment qaSeg: qaSegs) {
			if(qaSeg.getRefEnd() < afterCoord) {
				resultQaSegs.add(qaSeg.clone());
			} else if(qaSeg.getRefStart() >= afterCoord+numCols) {
				QueryAlignedSegment resultQaSeg = qaSeg.clone();
				resultQaSeg.translateRef(-numCols);
				resultQaSegs.add(resultQaSeg);
			} else if(qaSeg.getRefEnd() >= afterCoord+numCols) {
				if(qaSeg.getRefStart() < afterCoord) {
					QueryAlignedSegment resultQaLeftSeg = qaSeg.clone();
					resultQaLeftSeg.truncateRight( ( qaSeg.getRefEnd() - afterCoord ) + 1);
					resultQaSegs.add(resultQaLeftSeg);

					QueryAlignedSegment resultQaRightSeg = qaSeg.clone();
					resultQaRightSeg.truncateLeft( ( afterCoord+numCols ) - qaSeg.getRefStart()); 
					resultQaRightSeg.translateRef(-numCols);
					resultQaSegs.add(resultQaRightSeg);
				} else {
					QueryAlignedSegment resultQaSeg = qaSeg.clone();
					resultQaSeg.truncateLeft( ( afterCoord+numCols ) - qaSeg.getRefStart()); 
					resultQaSeg.translateRef(-numCols);
					resultQaSegs.add(resultQaSeg);
				}
			} else { // qaSeg.getRefEnd() < afterCoord+numCols
				if(qaSeg.getRefStart() < afterCoord) { 
					QueryAlignedSegment resultQaSeg = qaSeg.clone();
					resultQaSeg.truncateRight( ( qaSeg.getRefEnd() - afterCoord ) + 1);
					resultQaSegs.add(resultQaSeg);
				} else { // qaSeg.getRefStart() >= afterCoord 
					continue;
				}
			}
		}
		return resultQaSegs;
	}

	
	
	
	public static void checkLengths(List<QueryAlignedSegment> segs) {
		for(QueryAlignedSegment seg: segs) {
			if(seg.getRefEnd() - seg.getRefStart() != seg.getQueryEnd() - seg.getQueryStart()) {
				GlueLogger.getGlueLogger().finest("Invalid segment length: "+seg);
			}
		}
	}

	public static Integer minQueryStart(List<? extends IQueryAlignedSegment> segList) {
		return segList.stream().map(s -> s.getQueryStart()).min(Integer::compare).orElse(null);
	}

	public static Integer maxQueryEnd(List<? extends IQueryAlignedSegment> segList) {
		return segList.stream().map(s -> s.getQueryEnd()).max(Integer::compare).orElse(null);
	}

	// given segment mapping [a,b] on Query to [c,d] on reference returns segment
	// mapping [(qLength-b)+1, (qLength-a)+1] on Query to [(rLength-d)+1,(rLength-c)+1] on reference returns segment
	public QueryAlignedSegment reverseSense(int qLength, int rLength) {
		return new QueryAlignedSegment(
				reverseLocationSense(rLength, getRefEnd()), reverseLocationSense(rLength, getRefStart()), 
				reverseLocationSense(qLength, getQueryEnd()), reverseLocationSense(qLength, getQueryStart()));
	}
	
	public static List<QueryAlignedSegment> reverseSense(List<QueryAlignedSegment> qaSegs, int qLength, int rLength) {
		LinkedList<QueryAlignedSegment> results = new LinkedList<QueryAlignedSegment>();
		for(QueryAlignedSegment qaSeg: qaSegs) {
			results.push(qaSeg.reverseSense(qLength, rLength));
		}
		return results;
	}
	
	public static List<QueryAlignedSegment> cloneList(List<QueryAlignedSegment> segs) {
		return segs.stream().map(seg -> seg.clone()).collect(Collectors.toList());
	}

}