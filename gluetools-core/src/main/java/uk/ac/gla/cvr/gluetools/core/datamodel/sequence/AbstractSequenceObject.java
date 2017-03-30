package uk.ac.gla.cvr.gluetools.core.datamodel.sequence;

import java.util.List;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSettingOption;
import uk.ac.gla.cvr.gluetools.core.segments.IQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.IReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.NtReferenceSegment;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

public abstract class AbstractSequenceObject {

	private SequenceFormat seqFormat;
	private String processedNucleotides = null;
	
	public AbstractSequenceObject(SequenceFormat seqFormat) {
		super();
		this.seqFormat = seqFormat;
	}

	public SequenceFormat getSeqFormat() {
		return seqFormat;
	}

	public final String getNucleotides(CommandContext cmdContext) {
		if(processedNucleotides == null) {
			processedNucleotides = getNucleotidesInternal(cmdContext).toUpperCase();
			if(cmdContext.getProjectSettingValue(ProjectSettingOption.IGNORE_NT_SEQUENCE_HYPHENS).equals("true")) {
				processedNucleotides = processedNucleotides.replaceAll("-", "");
			}
		}
		return processedNucleotides;
	}
	
	protected abstract String getNucleotidesInternal(CommandContext cmdContext);
	
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
	public List<NtQueryAlignedSegment> getNtQueryAlignedSegments(List<? extends IQueryAlignedSegment> queryAlignedSegments, CommandContext cmdContext) {
		String nucleotides = getNucleotides(cmdContext);
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
	public List<NtReferenceSegment> getNtReferenceSegments(List<? extends IReferenceSegment> refSegments, CommandContext cmdContext) {
		String nucleotides = getNucleotides(cmdContext);
		return refSegments.stream()
				.map(refSegment -> {
					int refStart = refSegment.getRefStart();
					int refEnd = refSegment.getRefEnd();
					return new NtReferenceSegment(refStart, refEnd, 
							nucleotides.subSequence(refStart-1, refEnd));
				})
				.collect(Collectors.toList());
	}
	
	public CharSequence getNucleotides(CommandContext cmdContext, int ntStart, int ntEnd) {
		return getNucleotides(cmdContext).subSequence(ntStart-1, ntEnd);
	}

	public char nt(CommandContext cmdContext, int position) {
		String nucleotides = getNucleotides(cmdContext);
		return FastaUtils.nt(nucleotides, position);
	}

	public CharSequence subSequence(CommandContext cmdContext, int start, int end) {
		String nucleotides = getNucleotides(cmdContext);
		return FastaUtils.subSequence(nucleotides, start, end);
	}

	public int find(CommandContext cmdContext, String sequence, int from) {
		String nucleotides = getNucleotides(cmdContext);
		return FastaUtils.find(nucleotides, sequence, from);
	}

	
	
}
