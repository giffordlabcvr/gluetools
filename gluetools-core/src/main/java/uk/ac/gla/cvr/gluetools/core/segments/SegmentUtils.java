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

public class SegmentUtils {

	public static class Segment {
		public int start,end;
		public Segment(int start, int end) {
			super();
			this.start = start;
			this.end = end;
		}
	}
	
	
	public static Segment overlap(Segment seg1, Segment seg2) {
		if(seg1.start > seg2.end || seg2.start > seg1.end) {
			return null; // no overlap
		}
		int overlapStart = Math.max(seg1.start, seg2.start);
		int overlapEnd = Math.min(seg1.end, seg2.end);
		return new Segment(overlapStart, overlapEnd);
	}
	
	
	public static String base1CharAt(String seq, int position) {
		return seq.substring(position-1, position);
	}

	public static char base1Char(String seq, int position) {
		return seq.charAt(position-1);
	}

	public static String base1SubString(String seq, int start, int end) {
		if(start <= end) {
			return seq.substring(start-1, end);
		} else {
			return new StringBuilder(seq.substring(end-1, start)).reverse().toString();
		}
	}

	
}
