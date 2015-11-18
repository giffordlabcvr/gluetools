package uk.ac.gla.cvr.gluetools.core.reporting.contentNotes;

import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;

/**
 * A variation note points out that the sequence content contains a specific variation.
 */

public class VariationNote extends SequenceContentNote {

	private String variationName;

	public VariationNote(String variationName, int refStart, int refEnd) {
		super(refStart, refEnd);
		this.variationName = variationName;
	}

	@Override
	public void toDocument(ObjectBuilder sequenceDifferenceObj) {
		super.toDocument(sequenceDifferenceObj);
		sequenceDifferenceObj.set("variationName", getVariationName());
	}
	
	public String getVariationName() {
		return variationName;
	}

	public void setVariationName(String variationName) {
		this.variationName = variationName;
	}

	public VariationNote clone() {
		return new VariationNote(getVariationName(), getRefStart(), getRefEnd());
	}

	
}
