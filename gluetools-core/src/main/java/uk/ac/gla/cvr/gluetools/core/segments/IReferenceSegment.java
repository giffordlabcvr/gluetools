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

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import uk.ac.gla.cvr.gluetools.core.document.CommandObject;


public interface IReferenceSegment extends IReadOnlyReferenceSegment {

	public static final String REF_START = "refStart";
	public static final String REF_END = "refEnd";

	public void setRefStart(Integer refStart);
	
	public void setRefEnd(Integer refEnd);
	
	public static void checkTruncateLength(IReferenceSegment segment, int length) {
		int currentLength = segment.getCurrentLength();
		if(currentLength == 1) {
			throw new IllegalArgumentException("Segment of length 1 cannot be truncated");
		}
		int maxLength = currentLength - 1;
		if(length <= 0 || length > maxLength) {
			throw new IllegalArgumentException("Illegal length argument: "+
		length+": should be between "+1+" and "+maxLength+" inclusive" );
		}
	}

	public default void truncateLeft(int length) {
		checkTruncateLength(this, length);
		setRefStart(getRefStart()+length);
	}

	public default void truncateRight(int length) {
		checkTruncateLength(this, length);
		setRefEnd(getRefEnd()-length);
	}

	public IReferenceSegment clone();
	
	
	public default void toDocument(CommandObject builder) {
		builder
			.set(REF_START, getRefStart())
			.set(REF_END, getRefEnd());
	}

	public static <L extends List<S>, S extends IReferenceSegment> L sortByRefStart(L segments, Supplier<L> listSupplier) {
		L sorted = listSupplier.get();
		sorted.addAll(segments);
		Collections.sort(sorted, new RefStartComparator());
		return sorted;
	}
	
	public static class RefStartComparator implements Comparator<IReferenceSegment> {
		@Override
		public int compare(IReferenceSegment o1, IReferenceSegment o2) {
			return Integer.compare(o1.getRefStart(), o2.getRefStart());
		}
	}

	public default void translate(int offset) {
		setRefStart(getRefStart()+offset);
		setRefEnd(getRefEnd()+offset);
	}


	public static <S extends IReferenceSegment> Integer totalReferenceLength(List<S> segments) {
		int l = 0;
		for(S seg: segments) {
			l += seg.getCurrentLength();
		}
		return l;
	}
	
}
