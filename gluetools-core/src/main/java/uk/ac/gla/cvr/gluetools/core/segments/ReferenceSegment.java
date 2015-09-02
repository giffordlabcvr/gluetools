package uk.ac.gla.cvr.gluetools.core.segments;

import org.w3c.dom.Element;

import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectReader;
import uk.ac.gla.cvr.gluetools.core.plugins.Plugin;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginConfigContext;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginUtils;

public class ReferenceSegment implements Plugin, IReferenceSegment {

	
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
	
	protected void checkTruncateLength(int length) {
		if(length <= 0 || length > getRefEnd() - getRefStart()) {
			throw new IllegalArgumentException("Illegal length argument: "+
		length+": should be between "+1+" and "+(getRefEnd() - getRefStart())+" inclusive" );
		}
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
	
}
