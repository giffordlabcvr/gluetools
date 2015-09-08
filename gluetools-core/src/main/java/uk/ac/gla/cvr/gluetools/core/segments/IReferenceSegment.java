package uk.ac.gla.cvr.gluetools.core.segments;

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

	
	
}
