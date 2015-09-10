package uk.ac.gla.cvr.gluetools.core.segments;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;


public interface IReferenceSegment {

	public static final String REF_START = "refStart";
	public static final String REF_END = "refEnd";


	public Integer getRefStart();

	public Integer getRefEnd();
	
	public void setRefStart(Integer refStart);
	
	public void setRefEnd(Integer refEnd);
	
	public default int getCurrentLength() {
		return 1+(getRefEnd() - getRefStart());
	}
	
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
	
	
	public default void toDocument(ObjectBuilder builder) {
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
			return Integer.compare(o1.getRefStart(), o1.getRefStart());
		}
	}

	public default void translate(int offset) {
		setRefStart(getRefStart()+offset);
		setRefEnd(getRefEnd()+offset);
	}

	
}
