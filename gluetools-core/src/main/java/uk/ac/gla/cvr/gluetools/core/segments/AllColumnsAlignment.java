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
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.stream.Collectors;

import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import uk.ac.gla.cvr.gluetools.core.logging.GlueLogger;


public class AllColumnsAlignment<K> {

	private Map<K, List<QueryAlignedSegment>> keyToSegments = new LinkedHashMap<K, List<QueryAlignedSegment>>();
	
	private Integer maxIndex = null;
	
	public AllColumnsAlignment(K initialSeqKey, int initialSeqLength) {
		QueryAlignedSegment segment = new QueryAlignedSegment(1, initialSeqLength, 1, initialSeqLength);
		ArrayList<QueryAlignedSegment> segs = new ArrayList<QueryAlignedSegment>();
		segs.add(segment);
		keyToSegments.put(initialSeqKey, segs);
	}
	
	public List<QueryAlignedSegment> getSegments(K key) {
		return keyToSegments.get(key);
	}

	public List<K> getKeys() {
		return new ArrayList<K>(keyToSegments.keySet());
	}
	
	public Integer getMaxIndex() {
		if(maxIndex == null) {
			recalculateMaxIndex();
		}
		return maxIndex;
	}
	
	private void recalculateMaxIndex() {
		maxIndex = 1;
		for(List<QueryAlignedSegment> qaSegs: keyToSegments.values()) {
			maxIndex = Math.max(maxIndex, ReferenceSegment.maxRefEnd(qaSegs));
		}
		
	}

	public void addRow(K newKey, K refKey, List<QueryAlignedSegment> newToRefSegs, int newLen) {
		maxIndex = null;
		List<QueryAlignedSegment> refToUSegs = keyToSegments.get(refKey);
		List<QueryAlignedSegment> newToUSegs = QueryAlignedSegment.translateSegments(newToRefSegs, refToUSegs);
		
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
		QueryAlignedSegment lastUToNewSeg = null;
		for(QueryAlignedSegment uToNewSeg: uToNewSegs) {
			int newStart = lastCoveredNewLoc+1;
			if(uToNewSeg.getRefStart() > newStart) {
				ColumnInsertion colInsertion;
				Integer rightNT = uToNewSeg.getQueryStart();
				if(lastUToNewSeg == null) {
					colInsertion = 
							new ColumnInsertionBefore(newStart, uToNewSeg.getRefStart()-newStart, rightNT);
				} else {
					Integer leftNT = lastUToNewSeg.getQueryEnd();
					colInsertion = 
							new ColumnInsertionAfter(newStart, uToNewSeg.getRefStart()-newStart, leftNT);
				}
				colInsertions.add(colInsertion);
			}
			lastUToNewSeg = uToNewSeg;
			lastCoveredNewLoc = uToNewSeg.getRefEnd();
			lastULoc = uToNewSeg.getQueryEnd();
		}
		if(lastCoveredNewLoc < newLen) {
			int newStart = lastCoveredNewLoc+1;
			ColumnInsertion colInsertion = 
					new ColumnInsertionAfter(newStart, (newLen-newStart)+1, lastULoc);
				colInsertions.add(colInsertion);
		}
		// add new row to the alignment
		keyToSegments.put(newKey, newToUSegs);

		for(int i = 0; i < colInsertions.size(); i++) {
			ColumnInsertion colInsertion = colInsertions.get(i);
			int numCols = colInsertion.length;
			
			// apply column insertion to each row of the alignment
			keyToSegments.forEach( (seqId, seqToUSegs) -> {
				List<QueryAlignedSegment> newSeqToUSegs = new ArrayList<QueryAlignedSegment>(seqToUSegs);
				if(colInsertion instanceof ColumnInsertionBefore) {
					ColumnInsertionBefore colInsertionBefore = (ColumnInsertionBefore) colInsertion;
					newSeqToUSegs = QueryAlignedSegment
							.insertRefColumnsBefore(colInsertionBefore.rightNT, numCols, newSeqToUSegs);
				} else if(colInsertion instanceof ColumnInsertionAfter) {
					ColumnInsertionAfter colInsertionAfter = (ColumnInsertionAfter) colInsertion;
					newSeqToUSegs = QueryAlignedSegment
							.insertRefColumnsAfter(colInsertionAfter.leftNT, numCols, newSeqToUSegs);
				}
				seqToUSegs.clear();
				seqToUSegs.addAll(newSeqToUSegs);
			} );
			// determine reference coordinates for the column insertion
			if(colInsertion instanceof ColumnInsertionBefore) {
				ColumnInsertionBefore colInsertionBefore = (ColumnInsertionBefore) colInsertion;
				colInsertion.finalRefStart = colInsertionBefore.rightNT;
				colInsertion.finalRefEnd = (colInsertionBefore.rightNT + numCols)-1;
			} else if(colInsertion instanceof ColumnInsertionAfter) {
				ColumnInsertionAfter colInsertionAfter = (ColumnInsertionAfter) colInsertion;
				colInsertion.finalRefStart = colInsertionAfter.leftNT + 1;
				colInsertion.finalRefEnd = colInsertionAfter.leftNT + numCols;
			}
			// translate remaining column insertions
			for(int j = i+1; j < colInsertions.size(); j++) {
				colInsertions.get(j).translate(numCols);
			}
		}		
		// add new segments for the inserted regions for the new sequence.
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
	
	
	public static void initAllColumnsAlmt(Map<String, List<QueryAlignedSegment>> allColumnsAlmt, 
			String initialRefID, int length) {
		QueryAlignedSegment qaSeg = new QueryAlignedSegment(1, length, 1, length);
		List<QueryAlignedSegment> qaSegs = new ArrayList<QueryAlignedSegment>();
		qaSegs.add(qaSeg);
		allColumnsAlmt.put(initialRefID, qaSegs);
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
		public String toString() {
			return "insert "+length+" columns before "+rightNT+", starting from "+newStart+" on new sequence";
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
		public String toString() {
			return "insert "+length+" columns after "+leftNT+", starting from "+newStart+" on new sequence";
		}
	}

	private static class ColumnDeletionAfter {
		int leftNT;
		int length;
		public ColumnDeletionAfter(int leftNT, int length) {
			this.leftNT = leftNT;
			this.length = length;
		}
		public void translate(int amount) {
			leftNT += amount;
		}
		public String toString() {
			return "delete "+length+" columns after "+leftNT;
		}
	}

	
	public void rationalise() {
		keyToSegments.forEach((key, segs) -> {
			List<QueryAlignedSegment> merged = 
					QueryAlignedSegment.mergeAbutting(segs, QueryAlignedSegment.mergeAbuttingFunctionQueryAlignedSegment(), 
							QueryAlignedSegment.abutsPredicateQueryAlignedSegment());
			segs.clear();
			segs.addAll(merged);
		});
		
	}

	public List<QueryAlignedSegment> key1ToKey2Segments(K key1, K key2) {
		List<QueryAlignedSegment> key1ToUSegs = getSegments(key1);
		List<QueryAlignedSegment> key2ToUSegs = getSegments(key2);
		List<QueryAlignedSegment> uToKey2Segs = 
				key2ToUSegs.stream().map(seg -> seg.invert()).collect(Collectors.toList());
		return QueryAlignedSegment.translateSegments(key1ToUSegs, uToKey2Segs);
	}
	
	
	public void logRegionAllKeys(int startUIndex, int endUIndex, Level level) {
		GlueLogger.getGlueLogger().log(level, "Logging all-columns alignment region ["+startUIndex+", "+endUIndex+"]");
		ReferenceSegment refSeg = new ReferenceSegment(startUIndex, endUIndex);
		List<ReferenceSegment> loggedRegion = Arrays.asList(refSeg);
		keyToSegments.forEach((key, segs) -> {
			GlueLogger.getGlueLogger().log(level, key.toString()+": "+
					ReferenceSegment.intersection(segs, loggedRegion, ReferenceSegment.cloneLeftSegMerger()));
		});
	}

	
	public void logRegion(K key, int startUIndex, int endUIndex, Level level) {
		GlueLogger.getGlueLogger().log(level, "Logging all-columns alignment region ["+startUIndex+", "+endUIndex+"]");
		ReferenceSegment refSeg = new ReferenceSegment(startUIndex, endUIndex);
		List<ReferenceSegment> loggedRegion = Arrays.asList(refSeg);
		GlueLogger.getGlueLogger().log(level, key.toString()+": "+
				ReferenceSegment.intersection(keyToSegments.get(key), loggedRegion, ReferenceSegment.cloneLeftSegMerger()));
	}

	public void remove(K key) {
		keyToSegments.remove(key);
		this.maxIndex = null;
	}
	

	// remove those columns used by fewer than <minUsage> rows.
	// usageQualifier is a predicate which defines whether the row relating to a key counts as usage.
	public void removeUnderusedColumns(int minUsage, Predicate<K> usageQualfier) {
		TIntIntMap indexToUsage = new TIntIntHashMap();
		Integer initalMaxIndex = getMaxIndex();
		for(int i = 1; i <= initalMaxIndex; i++) {
			indexToUsage.put(i, 0);
		}
		keyToSegments.forEach( (key, segs) -> {
			segs.forEach(seg -> {
				if(usageQualfier.test(key)) {
					for(int i = seg.getRefStart(); i <= seg.getRefEnd(); i++) {
						indexToUsage.adjustValue(i, 1);
					}
				}
			});
		});
		List<ColumnDeletionAfter> columnDeletions = new ArrayList<ColumnDeletionAfter>();
		ColumnDeletionAfter currentColDeletion = null;
		for(int i = 1; i <= initalMaxIndex; i++) {
			if(indexToUsage.get(i) < minUsage) {
				if(currentColDeletion != null) {
					currentColDeletion.length++;
				} else {
					currentColDeletion = new ColumnDeletionAfter(i, 1);
					columnDeletions.add(currentColDeletion);
				}
			} else {
				if(currentColDeletion != null) {
					currentColDeletion = null;
				}
			}
		}
		
		for(int j = 0; j < columnDeletions.size(); j++) {
			ColumnDeletionAfter colDeletion = columnDeletions.get(j);
			keyToSegments.forEach((k, segs) -> {
				List<QueryAlignedSegment> segsCopy = new ArrayList<QueryAlignedSegment>(segs);
				segs.clear();
				segs.addAll(QueryAlignedSegment.deleteRefColumnsAfter(colDeletion.leftNT, colDeletion.length, segsCopy));
			});
			// shift all subsequent column deletions to the left, by the appropriate number of columns.
			for(int k = j+1; k < columnDeletions.size(); k++) {
				ColumnDeletionAfter subsequentColDeletion = columnDeletions.get(k);
				subsequentColDeletion.translate(-colDeletion.length);
			}
		}
		this.maxIndex = null;
	}
	
}
