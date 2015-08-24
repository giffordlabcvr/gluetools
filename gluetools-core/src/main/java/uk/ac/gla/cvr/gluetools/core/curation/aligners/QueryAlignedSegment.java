package uk.ac.gla.cvr.gluetools.core.curation.aligners;

import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;
import uk.ac.gla.cvr.gluetools.core.document.ObjectReader;


public class QueryAlignedSegment {
	private int refStart, refEnd, queryStart, queryEnd;

	public QueryAlignedSegment(int refStart, int refEnd, int queryStart, int queryEnd) {
		super();
		this.refStart = refStart;
		this.refEnd = refEnd;
		this.queryStart = queryStart;
		this.queryEnd = queryEnd;
	}
	public QueryAlignedSegment(ObjectReader objectReader) {
		this(objectReader.intValue("refStart"),
				objectReader.intValue("refEnd"),
				objectReader.intValue("queryStart"),
				objectReader.intValue("queryEnd"));
	}
	
	public int getRefStart() {
		return refStart;
	}
	public void setRefStart(int refStart) {
		this.refStart = refStart;
	}
	public int getRefEnd() {
		return refEnd;
	}
	public void setRefEnd(int refEnd) {
		this.refEnd = refEnd;
	}
	public int getQueryStart() {
		return queryStart;
	}
	public void setQueryStart(int queryStart) {
		this.queryStart = queryStart;
	}
	public int getQueryEnd() {
		return queryEnd;
	}
	public void setQueryEnd(int queryEnd) {
		this.queryEnd = queryEnd;
	}

	public String toString() { return
		"Ref: ["+getRefStart()+", "+getRefEnd()+"] "+
				"<-> Query: ["+getQueryStart()+", "+getQueryEnd()+"]";
	}
	
	/**
	 * returns true if the two segments propose the same offset between query and reference,
	 * 
	 * This is useful to know in the case where the reference ranges overlap: 
	 * in this case the segments can easily be merged.
	 */
	public boolean isAlignedTo(QueryAlignedSegment other) {
		return queryStart - refStart == other.queryStart - other.refStart;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + queryEnd;
		result = prime * result + queryStart;
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
		QueryAlignedSegment other = (QueryAlignedSegment) obj;
		if (queryEnd != other.queryEnd)
			return false;
		if (queryStart != other.queryStart)
			return false;
		if (refEnd != other.refEnd)
			return false;
		if (refStart != other.refStart)
			return false;
		return true;
	}

	public void toDocument(ObjectBuilder builder) {
		builder
			.set("refStart", getRefStart())
			.set("refEnd", getRefEnd())
			.set("queryStart", getQueryStart())
			.set("queryEnd", getQueryEnd());
	}
	
	
}