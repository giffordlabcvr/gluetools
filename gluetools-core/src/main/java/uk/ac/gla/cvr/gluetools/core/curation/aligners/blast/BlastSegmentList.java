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
package uk.ac.gla.cvr.gluetools.core.curation.aligners.blast;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.function.Function;

public class BlastSegmentList extends LinkedList<BlastAlignedSegment> {
	public BlastSegmentList(BlastAlignedSegment ... segments) {
		super(Arrays.asList(segments));
	}
	public BlastSegmentList(BlastSegmentList other) {
		super(other);
	}
	
	/**
	 * add segments from the provided list into this list. 
	 * segments in this list cannot be overwritten by segments in the provided list
	 * if they overlap on the reference or on the query.
	 * Also, segments on the provided list will be ignored if they do not respect the query sequence
	 * segment ordering which is implicit in this list.
	 * 
	 * The provided list will be emptied by this operation, and its segments may be modified.
	 * @param newSegments0
	 */
	public void mergeInSegmentList(BlastSegmentList newSegments0) {
	
		// remove new segments which overlap existing segments on the reference sequence
		BlastSegmentList existingSegments1 = new BlastSegmentList();
		BlastSegmentList newSegments1 = new BlastSegmentList();
	
		Function<BlastAlignedSegment, Integer> 
			getRefStart = BlastAlignedSegment::getRefStart,
			getRefEnd = BlastAlignedSegment::getRefEnd;
		
		BlastAlignedSegment.removeNewOverlaps(this, newSegments0, existingSegments1, newSegments1, 
				getRefStart, getRefEnd);
	
		// remove new segments which overlap existing segments on the query sequence
		BlastSegmentList existingSegments2 = new BlastSegmentList();
		BlastSegmentList newSegments2 = new BlastSegmentList();
	
		Function<BlastAlignedSegment, Integer> 
			getQueryStart = BlastAlignedSegment::getQueryStart,
			getQueryEnd = BlastAlignedSegment::getQueryEnd;
	
		BlastAlignedSegment.removeNewOverlaps(existingSegments1, newSegments1, existingSegments2, newSegments2, 
				getQueryStart, getQueryEnd);
		
		// final pass, merge existing and new: new segments should only be added to the merged list 
		// only if they conform to the query order.
		
		BlastSegmentList merged = new BlastSegmentList();
		
		int lastQueryEnd = 0;
		int nextQueryStart = BlastAlignedSegment.updateNextStart(existingSegments2, getQueryStart);
		while(!(existingSegments2.isEmpty() && newSegments2.isEmpty())) {
			int nextExistingRefStart = BlastAlignedSegment.updateNextStart(existingSegments2, getRefStart);
			int nextNewRefStart = BlastAlignedSegment.updateNextStart(newSegments2, getRefStart);
			if(nextExistingRefStart < nextNewRefStart) {
				BlastAlignedSegment existingSegment = existingSegments2.removeFirst();
				merged.add(existingSegment);
				lastQueryEnd = existingSegment.getQueryEnd();
				nextQueryStart = BlastAlignedSegment.updateNextStart(existingSegments2, getQueryStart);
			} else {
				BlastAlignedSegment newSegment = newSegments2.removeFirst();
				if(newSegment.getQueryStart() > lastQueryEnd &&
						newSegment.getQueryEnd() < nextQueryStart) {
					merged.add(newSegment);
				}
			}
		}
		this.addAll(merged);
	}
}