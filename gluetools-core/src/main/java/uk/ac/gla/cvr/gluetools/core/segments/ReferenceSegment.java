package uk.ac.gla.cvr.gluetools.core.segments;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectReader;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public class ReferenceSegment implements Plugin, IReferenceSegment, Cloneable {

	
	public static final String REF_START = "refStart";
	public static final String REF_END = "refEnd";

	private int refStart, refEnd;

	public ReferenceSegment(int refStart, int refEnd) {
		super();
		this.refStart = refStart;
		this.refEnd = refEnd;
	}
	public ReferenceSegment(ObjectReader objectReader) {
		this(objectReader.intValue(REF_START),
				objectReader.intValue(REF_END));
	}
	
	public ReferenceSegment(PluginConfigContext pluginConfigContext,
			Element configElem) {
		configure(pluginConfigContext, configElem);
	}
	
	protected ReferenceSegment() {
	}
	
	@Override
	public void configure(PluginConfigContext pluginConfigContext,
			Element configElem) {
		setRefStart(PluginUtils.configureIntProperty(configElem, REF_START, true));
		setRefEnd(PluginUtils.configureIntProperty(configElem, REF_END, true));
	}
	
	@Override
	public Integer getRefStart() {
		return refStart;
	}
	public void setRefStart(int refStart) {
		this.refStart = refStart;
	}
	@Override
	public Integer getRefEnd() {
		return refEnd;
	}
	
	public void setRefEnd(int refEnd) {
		this.refEnd = refEnd;
	}
	
	public void truncateLeft(int length) {
		checkTruncateLength(length);
		refStart+=length;
	}

	public void truncateRight(int length) {
		checkTruncateLength(length);
		refEnd-=length;
	}

	
	protected void checkTruncateLength(int length) {
		if(length <= 0 || length > ( getCurrentLength() - 1 )) {
			throw new IllegalArgumentException("Illegal length argument: "+
		length+": should be between "+1+" and "+getCurrentLength()+" inclusive" );
		}
	}
	
	public int getCurrentLength() {
		return 1+(getRefEnd() - getRefStart());
	}

	public String toString() { return
		"Ref: ["+getRefStart()+", "+getRefEnd()+"]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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
		ReferenceSegment other = (ReferenceSegment) obj;
		if (refEnd != other.refEnd)
			return false;
		if (refStart != other.refStart)
			return false;
		return true;
	}

	public void toDocument(ObjectBuilder builder) {
		builder
			.set(REF_START, getRefStart())
			.set(REF_END, getRefEnd());
	}
	
	public ReferenceSegment clone() {
		return new ReferenceSegment(refStart, refEnd);
	}
	
	/**
	 * Split a segment into two parts, a new left part of length <length>
	 * which is returned
	 * This supplied segment is then modified to be the remaining part.
	 */
	public static <A extends ReferenceSegment> A truncateLeftSplit(A segment, int length) {
		@SuppressWarnings("unchecked")
		A newSegment = (A) segment.clone();
		int currentLength = segment.getCurrentLength();
		segment.truncateLeft(length);
		newSegment.truncateRight(currentLength-length);
		return newSegment;
	}
	
	/**
	 * Split a segment into two parts, a new right part of length <length>
	 * which is returned.
	 * The supplied segment is modified to be the remaining part.
	 */
	public static <A extends ReferenceSegment> A truncateRightSplit(A segment, int length) {
		@SuppressWarnings("unchecked")
		A newSegment = (A) segment.clone();
		int currentLength = segment.getCurrentLength();
		segment.truncateRight(length);
		newSegment.truncateLeft(currentLength-length);
		return newSegment;
	}
}
