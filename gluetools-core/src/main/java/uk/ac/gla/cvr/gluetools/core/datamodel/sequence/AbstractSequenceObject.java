package uk.ac.gla.cvr.gluetools.core.datamodel.sequence;

import java.util.List;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.segments.IQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.NtReferenceSegment;

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

	// Given segments aligning this sequence to a reference, return a set of NtReferenceSegments which 
	// contain nucleotide segments from this sequence, in the coordinates of that reference.
	public List<NtReferenceSegment> getNtReferenceSegments(List<? extends IQueryAlignedSegment> queryAlignedSegments) {
		String nucleotides = getNucleotides();
		return queryAlignedSegments.stream()
				.map(queryAlignedSegment -> {
					int refStart = queryAlignedSegment.getRefStart();
					int refEnd = queryAlignedSegment.getRefEnd();
					int queryStart = queryAlignedSegment.getQueryStart();
					int queryEnd = queryAlignedSegment.getQueryEnd();
					return new NtReferenceSegment(refStart, refEnd, 
							nucleotides.subSequence(queryStart-1, queryEnd));
				})
				.collect(Collectors.toList());
	}

}
