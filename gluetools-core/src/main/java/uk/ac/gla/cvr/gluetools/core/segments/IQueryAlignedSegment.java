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