package uk.ac.gla.cvr.gluetools.core.reporting;

import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;

public class SequenceDifference {
	private String variationName;
	private int refStart;
	private int refEnd;
	
	public SequenceDifference(String variationName, int refStart, int refEnd) {
		super();
		this.variationName = variationName;
		this.refStart = refStart;
		this.refEnd = refEnd;
	}

	public void toDocument(ObjectBuilder sequenceDifferenceObj) {
		if(variationName != null) {
			sequenceDifferenceObj.set("variationName", variationName);
		}
		sequenceDifferenceObj.set("refStart", refStart);
		sequenceDifferenceObj.set("refEnd", refEnd);
	}
	
}