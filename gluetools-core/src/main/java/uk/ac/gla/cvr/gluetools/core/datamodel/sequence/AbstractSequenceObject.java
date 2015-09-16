package uk.ac.gla.cvr.gluetools.core.datamodel.sequence;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.biojava.nbio.core.sequence.DNASequence;

import uk.ac.gla.cvr.gluetools.core.segments.IQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.IReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.NtReferenceSegment;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

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

	/**
	 * Given segments aligning this sequence to a reference, return a set of NtQueryAlignedSegments which 
	 * additionally contain nucleotide segments from this sequence.
	 * @param queryAlignedSegments
	 */
	public List<NtQueryAlignedSegment> getNtQueryAlignedSegments(List<? extends IQueryAlignedSegment> queryAlignedSegments) {
		String nucleotides = getNucleotides();
		return queryAlignedSegments.stream()
				.map(queryAlignedSegment -> {
					int refStart = queryAlignedSegment.getRefStart();
					int refEnd = queryAlignedSegment.getRefEnd();
					int queryStart = queryAlignedSegment.getQueryStart();
					int queryEnd = queryAlignedSegment.getQueryEnd();
					return new NtQueryAlignedSegment(refStart, refEnd, queryStart, queryEnd,
							nucleotides.subSequence(queryStart-1, queryEnd));
				})
				.collect(Collectors.toList());
	}

	
	/**
	 * Assuming this sequence is a reference sequence, create nucleotide segments from it, 
	 * according to the supplied reference segments
	 * @param refSegments
	 */
	public List<NtReferenceSegment> getNtReferenceSegments(List<? extends IReferenceSegment> refSegments) {
		String nucleotides = getNucleotides();
		return refSegments.stream()
				.map(refSegment -> {
					int refStart = refSegment.getRefStart();
					int refEnd = refSegment.getRefEnd();
					return new NtReferenceSegment(refStart, refEnd, 
							nucleotides.subSequence(refStart-1, refEnd));
				})
				.collect(Collectors.toList());
	}
	
	public CharSequence getNucleotides(int ntStart, int ntEnd) {
		return getNucleotides().subSequence(ntStart-1, ntEnd);
	}

	public static List<AbstractSequenceObject> seqObjectsFromSeqData(
			byte[] sequenceData) {
		SequenceFormat format = SequenceFormat.detectFormatFromBytes(sequenceData);
		List<AbstractSequenceObject> seqObjects;
		if(format == SequenceFormat.FASTA) {
			Map<String, DNASequence> fastaMap = FastaUtils.parseFasta(sequenceData);
			seqObjects = fastaMap.entrySet().stream()
					.map(ent -> new FastaSequenceObject(ent.getKey(), ent.getValue().toString()))
					.collect(Collectors.toList());
		} else {
			AbstractSequenceObject seqObj = format.sequenceObject();
			seqObj.fromOriginalData(sequenceData);
			seqObjects = Collections.singletonList(seqObj);
		}
		return seqObjects;
	}
	
}
