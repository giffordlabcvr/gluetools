package uk.ac.gla.cvr.gluetools.core.datamodel.sequence;

public abstract class AbstractSequenceObject {

	private SequenceFormat seqFormat;
	
	public AbstractSequenceObject(SequenceFormat seqFormat) {
		super();
		this.seqFormat = seqFormat;
	}

	public SequenceFormat getSeqFormat() {
		return seqFormat;
	}

	public abstract String getNucleotides();
	
	public abstract byte[] toOriginalData();

	public abstract void fromOriginalData(byte[] originalData);

	/*
	 * Either override both of these or neither!
	 */
	public byte[] toPackedData() {
		return toOriginalData();
	};
	
	public void fromPackedData(byte[] packedData) {
		fromOriginalData(packedData);
	}
	
	public abstract String getHeader();
}
