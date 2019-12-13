/**
 *    GLUE: A flexible system for virus sequence data
 *    Copyright (C) 2018 The University of Glasgow
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU Affero General Public License as published
 *    by the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU Affero General Public License for more details.

 *    You should have received a copy of the GNU Affero General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *    Contact details:
 *    MRC-University of Glasgow Centre for Virus Research
 *    Sir Michael Stoker Building, Garscube Campus, 464 Bearsden Road, 
 *    Glasgow G61 1QH, United Kingdom
 *    
 *    Josh Singer: josh.singer@glasgow.ac.uk
 *    Rob Gifford: robert.gifford@glasgow.ac.uk
*/
package uk.ac.gla.cvr.gluetools.core.datamodel.sequence;

import java.util.List;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.datamodel.projectSetting.ProjectSettingOption;
import uk.ac.gla.cvr.gluetools.core.datamodel.sequence.SequenceException.Code;
import uk.ac.gla.cvr.gluetools.core.segments.IQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.IReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.NtQueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.NtReferenceSegment;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;

public abstract class AbstractSequenceObject implements NucleotideContentProvider {

	private SequenceFormat seqFormat;
	private String processedNucleotides = null;
	// whether reverse complement / rotation was applied to processedNucleotides
	private boolean reverseComplementApplied = true;
	private boolean rotationApplied = true;
	private Sequence sequence;
	
	public AbstractSequenceObject(SequenceFormat seqFormat, Sequence sequence) {
		super();
		this.seqFormat = seqFormat;
		this.sequence = sequence;
	}

	public SequenceFormat getSeqFormat() {
		return seqFormat;
	}

	public final String getNucleotides(CommandContext cmdContext) {
		return getNucleotides(cmdContext, true, true);
	}

	public final String getNucleotides(CommandContext cmdContext, boolean applyReverseComplement, boolean applyRotation) {
		if(applyReverseComplement != reverseComplementApplied || applyRotation != rotationApplied) {
			processedNucleotides = null;
			reverseComplementApplied = applyReverseComplement;
			rotationApplied = applyRotation;
		}
		if(processedNucleotides == null) {
			processedNucleotides = getNucleotidesInternal(cmdContext).toUpperCase();
			if(cmdContext.getProjectSettingValue(ProjectSettingOption.IGNORE_NT_SEQUENCE_HYPHENS).equals("true")) {
				processedNucleotides = processedNucleotides.replaceAll("-", "");
			}
			if(applyReverseComplement) {
				String reverseComplementFieldName = cmdContext.getProjectSettingValue(ProjectSettingOption.SEQUENCE_REVERSE_COMPLEMENT_BOOLEAN_FIELD);
				if(reverseComplementFieldName != null) {
					Object reverseComplementFieldValueObj = sequence.readProperty(reverseComplementFieldName);
					if(reverseComplementFieldValueObj != null) {
						if(reverseComplementFieldValueObj instanceof Boolean) {
							if((Boolean) reverseComplementFieldValueObj) {
								processedNucleotides = FastaUtils.reverseComplement(processedNucleotides);
							}
						} else {
							throw new SequenceException(Code.SEQUENCE_FIELD_ERROR, "Sequence field '"+reverseComplementFieldName+"' must be of type BOOLEAN");
						}
					}
				}
			}
			if(applyRotation) {
				String rotationFieldName = cmdContext.getProjectSettingValue(ProjectSettingOption.SEQUENCE_ROTATION_INTEGER_FIELD);
				if(rotationFieldName != null) {
					Object rotationFieldValueObj = sequence.readProperty(rotationFieldName);
					if(rotationFieldValueObj != null) {
						if(rotationFieldValueObj instanceof Integer) {
							Integer rotationFieldValueInt = (Integer) rotationFieldValueObj;
							int ntLength = processedNucleotides.length();
							if(rotationFieldValueInt < 0 || rotationFieldValueInt >= ntLength ) {
								throw new SequenceException(Code.SEQUENCE_FIELD_ERROR, "Rotation field value "+rotationFieldValueInt+
										" out of range for sequence "+sequence.getSource().getName()+"/"+sequence.getSequenceID());
							}
							if(rotationFieldValueInt > 0) {
								processedNucleotides = rightRotate(processedNucleotides, rotationFieldValueInt);
							}
						} else {
							throw new SequenceException(Code.SEQUENCE_FIELD_ERROR, "Sequence field '"+rotationFieldName+"' must be of type INTEGER");
						}
					}
				}
			}
		}

		return processedNucleotides;
	}
	
	 // rotates s towards left by d  
    private String leftRotate(String str, int d) { 
    	return str.substring(d) + str.substring(0, d); 
    } 
  
    // rotates s towards right by d  
    private String rightRotate(String str, int d) { 
    	return leftRotate(str, str.length() - d); 
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
