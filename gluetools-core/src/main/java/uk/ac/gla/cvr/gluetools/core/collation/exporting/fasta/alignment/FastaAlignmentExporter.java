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
package uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.alignment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import uk.ac.gla.cvr.gluetools.core.collation.exporting.fasta.memberSupplier.AbstractMemberSupplier;
import uk.ac.gla.cvr.gluetools.core.command.CommandContext;
import uk.ac.gla.cvr.gluetools.core.command.CommandException;
import uk.ac.gla.cvr.gluetools.core.command.CommandException.Code;
import uk.ac.gla.cvr.gluetools.core.datamodel.GlueDataObject;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignment.Alignment;
import uk.ac.gla.cvr.gluetools.core.datamodel.alignmentMember.AlignmentMember;
import uk.ac.gla.cvr.gluetools.core.datamodel.refSequence.ReferenceSequence;
import uk.ac.gla.cvr.gluetools.core.plugins.PluginClass;
import uk.ac.gla.cvr.gluetools.core.segments.QueryAlignedSegment;
import uk.ac.gla.cvr.gluetools.core.segments.ReferenceSegment;
import uk.ac.gla.cvr.gluetools.core.segments.SegmentUtils;
import uk.ac.gla.cvr.gluetools.utils.FastaUtils;
import uk.ac.gla.cvr.gluetools.utils.fasta.DNASequence;

@PluginClass(elemName="fastaAlignmentExporter",
description="Exports nucleotide alignments to a FASTA file")
public class FastaAlignmentExporter extends AbstractFastaAlignmentExporter<FastaAlignmentExporter> {
	
	public FastaAlignmentExporter() {
		super();
		registerModulePluginCmdClass(FastaAlignmentExportCommand.class);
		registerModulePluginCmdClass(FastaAlignmentWebExportCommand.class);
		
	}



	public static void exportAlignment(
			CommandContext cmdContext, IAlignmentColumnsSelector alignmentColumnsSelector,
			Boolean excludeEmptyRows, 
			AbstractMemberSupplier memberSupplier, AbstractStringAlmtRowConsumer almtRowConsumer) {

		// reference segments defining the output columns, coordinate space may be based on a reference (the translateToRef) or not.
		List<ReferenceSegment> outputRefSegs = initOutputRefSegs(cmdContext, memberSupplier, alignmentColumnsSelector);

		// name of a related reference that the member QA segs must be translated to before applying the output ref segs, may be null.
		String translateToRefName = null;
		Alignment alignment = memberSupplier.supplyAlignment(cmdContext);
		if(alignmentColumnsSelector != null) {
			String selectorRelatedRefName = alignmentColumnsSelector.getRelatedRefName();
			if(selectorRelatedRefName != null) {
				translateToRefName = alignment.getRelatedRef(cmdContext, selectorRelatedRefName).getName();
			} else {
				if(alignment.isConstrained()) {
					throw new CommandException(Code.COMMAND_FAILED_ERROR, "Unconstrained columns selector may only be used on an unconstrained alignment");
				}
			}
		} else if(alignment.isConstrained()) {
			translateToRefName = alignment.getConstrainingRef().getName();
		}
		
		int numMembers = memberSupplier.countMembers(cmdContext);
		//GlueLogger.getGlueLogger().log(Level.FINEST, "processing "+numMembers+" alignment members");
		int offset = 0;
		//int processed = 0;
		int batchSize = 500;
		while(offset < numMembers) {
			alignment = memberSupplier.supplyAlignment(cmdContext);
			List<AlignmentMember> almtMembers = memberSupplier.supplyMembers(cmdContext, offset, batchSize);
			ReferenceSequence translateToRef = null;
			if(translateToRefName != null) {
				translateToRef = GlueDataObject.lookup(cmdContext, ReferenceSequence.class, ReferenceSequence.pkMap(translateToRefName));
			}
			createAlignment(cmdContext, excludeEmptyRows, translateToRef, outputRefSegs, almtMembers, almtRowConsumer);
			//processed += almtMembers.size();
			//GlueLogger.getGlueLogger().log(Level.FINEST, "processed "+processed+" alignment members");
			offset += batchSize;
			cmdContext.newObjectContext();
		}
		
	}

	// set of refsegs that define the nucleotide column of the output.
	private static List<ReferenceSegment> initOutputRefSegs(CommandContext cmdContext, AbstractMemberSupplier memberSupplier, IAlignmentColumnsSelector alignmentColumnsSelector) {
		if(alignmentColumnsSelector != null) {
			return alignmentColumnsSelector.selectAlignmentColumns(cmdContext).stream().map(frs -> frs.clone()).collect(Collectors.toList());
		} else {
			// no columns selector, return list containing a single segment spanning the whole alignment width.
			ReferenceSegment minMaxSeg = new ReferenceSegment(1, 1);
			ReferenceSequence alignmentRef = memberSupplier.supplyAlignment(cmdContext).getRefSequence();
			if(alignmentRef != null) {
				// constrained alignment
				minMaxSeg.setRefEnd(alignmentRef.getSequence().getSequenceObject().getNucleotides(cmdContext).length());
			} else {
				// unconstrained alignment
				int numMembers = memberSupplier.countMembers(cmdContext);
				int offset = 0;
				int batchSize = 500;
				while(offset < numMembers) {
					List<AlignmentMember> almtMembers = memberSupplier.supplyMembers(cmdContext, offset, batchSize);
					for(AlignmentMember almtMember: almtMembers) {
						Integer maxRefEnd = ReferenceSegment.maxRefEnd(almtMember.getAlignedSegments());
						if(maxRefEnd != null) { minMaxSeg.setRefEnd(Math.max(maxRefEnd, minMaxSeg.getRefEnd())); }
					}
					offset += batchSize;
					cmdContext.newObjectContext();
				}
			}
			return Arrays.asList(minMaxSeg);
		}
	}
	
	private static void createAlignment(CommandContext cmdContext, Boolean excludeEmptyRows,
			ReferenceSequence translateToRef, List<ReferenceSegment> outputRefSegs, List<AlignmentMember> almtMembers, 
			AbstractStringAlmtRowConsumer almtRowConsumer) {

		for(AlignmentMember almtMember: almtMembers) {
			List<QueryAlignedSegment> memberQaSegs = almtMember.segmentsAsQueryAlignedSegments();
			if(translateToRef != null) {
				Alignment memberAlmt = almtMember.getAlignment();
				memberQaSegs = memberAlmt.translateToRelatedRef(cmdContext, memberQaSegs, translateToRef);
			}
			List<QueryAlignedSegment> coveredRegionSegs = ReferenceSegment.intersection(memberQaSegs, outputRefSegs, ReferenceSegment.cloneLeftSegMerger());
			List<ReferenceSegment> nonCoveredRegionSegs = ReferenceSegment.subtract(outputRefSegs, coveredRegionSegs);
			
			String memberNTs = almtMember.getSequence().getSequenceObject().getNucleotides(cmdContext);
			int alWidth = 0;
			for(ReferenceSegment constrainingRefSeg: outputRefSegs) {
				alWidth += constrainingRefSeg.getCurrentLength();
			}
			StringBuffer alignmentRow = new StringBuffer(alWidth);
			
			List<ReferenceSegment> mixedRegionSegs = new ArrayList<ReferenceSegment>();
			mixedRegionSegs.addAll(coveredRegionSegs);
			mixedRegionSegs.addAll(nonCoveredRegionSegs);
			ReferenceSegment.sortByRefStart(mixedRegionSegs);		
			
			
			for(ReferenceSegment seg: mixedRegionSegs) {
				if(seg instanceof QueryAlignedSegment) {
					QueryAlignedSegment qaSeg = (QueryAlignedSegment) seg;
					alignmentRow.append(SegmentUtils.base1SubString(memberNTs, qaSeg.getQueryStart(), qaSeg.getQueryEnd()));
				} else {
					for(int ntIndex = seg.getRefStart(); ntIndex <= seg.getRefEnd(); ntIndex++) {
						alignmentRow.append("-");
					}
				}
			}
			if( (!coveredRegionSegs.isEmpty()) || !excludeEmptyRows) {
				almtRowConsumer.consumeAlmtRow(cmdContext, almtMember, alignmentRow.toString());
			} 
	    }
	}

	public static Map<Map<String, String>, DNASequence> exportAlignment(
			CommandContext cmdContext,
			IAlignmentColumnsSelector alignmentColumnsSelector,
			boolean excludeEmptyRows, AbstractMemberSupplier memberSupplier) {
		Map<Map<String,String>, DNASequence> pkMapToRowDnaSeq = new LinkedHashMap<Map<String,String>, DNASequence>();
		exportAlignment(cmdContext, alignmentColumnsSelector, excludeEmptyRows, memberSupplier, new AbstractStringAlmtRowConsumer() {
			@Override
			public void consumeAlmtRow(CommandContext cmdContext, AlignmentMember almtMember, String alignmentRowString) {
				pkMapToRowDnaSeq.put(almtMember.pkMap(), FastaUtils.ntStringToSequence(alignmentRowString));
			}
		});
		return pkMapToRowDnaSeq;
	}
	
	
	
}
