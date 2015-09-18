package uk.ac.gla.cvr.gluetools.core.reporting.contentNotes;

import uk.ac.gla.cvr.gluetools.core.document.ObjectBuilder;

/**
 * A reference difference note points out that the sequence content differs from a reference at certain locations.
 * For efficiency, these differences are encoded in a mask char sequence. In the mask, "-" represents no difference and 
 * "X" represents a difference.
 */

public class ReferenceDifferenceNote extends SequenceContentNote {

	private CharSequence mask;

	public ReferenceDifferenceNote(int refStart, int refEnd, CharSequence mask) {
		super(refStart, refEnd);
		this.mask = mask;
	}

	@Override
	public void toDocument(ObjectBuilder sequenceDifferenceObj) {
		super.toDocument(sequenceDifferenceObj);
		sequenceDifferenceObj.set("mask", mask);
	}

	@Override
	public ReferenceDifferenceNote clone() {
		return new ReferenceDifferenceNote(getRefStart(), getRefEnd(), getMask());
	}

	public CharSequence getMask() {
		return mask;
	}

	public void setMask(CharSequence maskString) {
		this.mask = maskString;
	}

	@Override
	public void truncateLeft(int length) {
		super.truncateLeft(length);
		setMask(getMask().subSequence(length, getMask().length()));
	}

	@Override
	public void truncateRight(int length) {
		super.truncateRight(length);
		setMask(getMask().subSequence(0, getMask().length() - length));
	}
}
